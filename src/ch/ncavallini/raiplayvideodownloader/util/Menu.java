package ch.ncavallini.raiplayvideodownloader.util;
import java.io.PrintStream;
import java.util.Scanner;

/**
 * Utility class to create a console-based menu.
 * @author Niccolò Cavallini
 *
 */
public class Menu {
	
	public static int menu(Scanner input, String...options) {
		return menu(input, System.out, options);
	}
	
	public static int menu(Scanner input, PrintStream out, String... options) {
		out.println("Choose an option:\n");
		
		for(int i=0; i < options.length; i++) {
			out.printf("[%d]\t%s\n", i, options[i]);
		}
		int choice  = input.nextInt();
		return (choice >= 0 && choice < options.length) ? choice : menu(input, out, options);
	}

}
