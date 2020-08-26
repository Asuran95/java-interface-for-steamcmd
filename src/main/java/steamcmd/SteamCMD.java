package steamcmd;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;

import com.pty4j.PtyProcess;

public class SteamCMD {
	
	private PtyProcess pty;
	
	private Semaphore semaphore = new Semaphore(1);
	
	private List<SteamCMDListener> listeners = new ArrayList<>();
	
	private String steamCmdLocal;
	
	public SteamCMD() throws IOException, InterruptedException {
		startSteamCmd();
	}
	
	public SteamCMD(String steamCmdLocal) throws IOException, InterruptedException {
		this.steamCmdLocal = steamCmdLocal;
		startSteamCmd();
	}
	
	public SteamCMD(SteamCMDListener listener) throws IOException, InterruptedException {
		listeners.add(listener);
		startSteamCmd();
	}
	
	public SteamCMD(SteamCMDListener listener, String steamCmdLocal) throws IOException, InterruptedException {
		this.steamCmdLocal = steamCmdLocal;
		listeners.add(listener);
		startSteamCmd();
	}
	
	public void addListener(SteamCMDListener listener) {
		listeners.add(listener);
	}
	
	private void startSteamCmd() throws IOException, InterruptedException {
		
		if(steamCmdLocal == null) {
			String os = System.getProperty("os.name").toLowerCase();
			
			if(os.contains("linux")) {
				String localDir = "/tmp/steamcmd/";
				String zipSteam = "steamcmd_linux.tar.gz";
				String steamcmdURL = "https://steamcdn-a.akamaihd.net/client/installer/steamcmd_linux.tar.gz";
				steamCmdLocal = localDir + "steamcmd.sh";
				
				File shellSteamcmd = new File(steamCmdLocal);
				
				if(!shellSteamcmd.exists()) {
					File localFile = new File(localDir);
					File zipFile = new File(localDir + zipSteam);
					
					localFile.mkdir();
				
					URL url = new URL(steamcmdURL);
			        ReadableByteChannel rbc = Channels.newChannel(url.openStream());
			        FileOutputStream fos = new FileOutputStream(zipFile);
			        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			        fos.close();
			        rbc.close();
			             
			        Archiver archiver = ArchiverFactory.createArchiver("tar", "gz");
			        archiver.extract(zipFile, localFile);
				}    
			} else if(os.contains("win")) {
				
			}
		}
		
		String[] commandLines = { steamCmdLocal };

		this.pty = PtyProcess.exec(commandLines);
		
        InputStream stdout = pty.getInputStream();   
        BufferedReader reader = new BufferedReader(new InputStreamReader(stdout), 1);
        
        semaphore.acquire();
        
        new Thread() {

            @Override
            public void run() {
                try {
                    while (true) {
                    	String out = reader.readLine();
                    	
                        if (out == null) {
                        	semaphore.release();
                            break;
                        } else if(out.contains("[1m")) {
                        	System.err.println("Unlock Sem");
                        	semaphore.release();
                        } else if(out.contains("code from your Steam Guard") || out.contains("This computer has not been authenticated")) {
                        	
                        	String authCode = "";
                        	
                			for(SteamCMDListener l : listeners) {
                				authCode = l.onAuthCode();
                        	}
                        			
                        	OutputStream stdin = pty.getOutputStream();
                    		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
                    		
                    		writer.write(authCode + "\n");
                            writer.flush();
                        		
                        } else if(out.contains("Two-factor code mismatch") || out.contains("Invalid Login Auth Code")) {                      	
                        	for(SteamCMDListener l : listeners) {
                        		l.onFailedLoginCode();
                        	}  	
                        } else if(out.contains("Invalid Password")) {                      	
                        	for(SteamCMDListener l : listeners) {
                        		l.onInvalidPassword();
                        	}  	
                        } else { 	
                        	for(SteamCMDListener l : listeners) {
                        		l.onStdOut(out);
                        	}
                        }     
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        
        semaphore.acquire();
	}
	
	public void login(String username, String password) throws InterruptedException, IOException {
		login(username, password, "");
	}
	
	public void login(String username, String password, String authCode) throws InterruptedException, IOException {
		
		String loginCommand = "login " + username + " " + password + " " + authCode;
		
		OutputStream stdin = pty.getOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
		
		writer.write(loginCommand + "\n");
        writer.flush();
        
        semaphore.acquire();
	}
	
	public void loginAnonymous() throws InterruptedException, IOException {
		
		String loginCommand = "login anonymous";
		
		OutputStream stdin = pty.getOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
		
		writer.write(loginCommand + "\n");
        writer.flush();
        
        semaphore.acquire();
	}
	
	public void forceInstallDir(String dir) throws InterruptedException, IOException {
		
		String installDirCommand = "force_install_dir " + dir;
		
		OutputStream stdin = pty.getOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
		
		writer.write(installDirCommand + "\n");
        writer.flush();
        
        semaphore.acquire();
	}
	
	public void appUpdate(long appId) throws IOException, InterruptedException {
		appUpdate(appId, "");
	}
	
	public void appUpdate(long appId, String... flags) throws IOException, InterruptedException {
		
		String appUpdateCommand = "app_update " + appId;
		
		for(String f : flags) {
			appUpdateCommand += " " + f;
		}
		
		OutputStream stdin = pty.getOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
		
		writer.write(appUpdateCommand + "\n");
        writer.flush();
        
        semaphore.acquire();
	}
	
	public void appSetConfig(long appId, String key, String value) throws IOException, InterruptedException {
		
		String appSetConfigCommand = "app_set_config " + appId + " " + key + " " + value;
		
		OutputStream stdin = pty.getOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
		
		writer.write(appSetConfigCommand + "\n");
        writer.flush();
        
        semaphore.acquire();
	}
	
	public void quit() throws InterruptedException, IOException {
		
		OutputStream stdin = pty.getOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
		
		writer.write("quit" + "\n");
        writer.flush();
        
        semaphore.acquire();
	}	
}
