package example;
import java.io.IOException;
import java.util.Scanner;

import steamcmd.SteamCMD;
import steamcmd.SteamCMDListener;

public class Main {

	public static void main(String[] args) throws IOException, InterruptedException {
		
		//SteamCMD steamCmd = new SteamCMD(new listenerSteamCMD(), "/home/pendragon/teste/steam/steamcmd.sh");
		SteamCMD steamCmd = new SteamCMD(new listenerSteamCMD());
		
		steamCmd.login("alp_intel", "rty23589");
		/*
		steamCmd.loginAnonymous();

		steamCmd.forceInstallDir("/home/pendragon/teste/cstrike1");

		steamCmd.appUpdate(90);

		steamCmd.forceInstallDir("/home/pendragon/teste/tfc");

		steamCmd.appSetConfig(90, "mod", "tfc");
		
		steamCmd.appUpdate(90, "-validate");

		steamCmd.quit();*/
	}

}


class listenerSteamCMD implements SteamCMDListener {

	@Override
	public void onStdOut(String out) {
		System.out.println(out);
	}

	@Override
	public String onAuthCode() {
		
		System.out.print("Two-factor code:");
		Scanner sc = new Scanner(System.in);
		
		return sc.nextLine();
	}

	@Override
	public void onFailedLoginCode() {
		System.err.println("FAILED login with result code Two-factor code mismatch");	
	}

	@Override
	public void onInvalidPassword() {
		System.err.println("FAILED login with result code Invalid Password");		
	}
}
