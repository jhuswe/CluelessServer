package controller;

import java.io.BufferedReader;
import java.io.PrintWriter;
import objects.Player;

public class InOut {
	public PrintWriter out;
	public BufferedReader in;
	public Player player;
	
	public InOut(BufferedReader in, PrintWriter out) {
		this.in = in;
		this.out = out;
	}
}
