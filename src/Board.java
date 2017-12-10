/*
*    Brick Breaker, Version 1.2
*    By Ty-Lucas Kelley
*	
*	 **LICENSE**
*
*	 This file is a part of Brick Breaker.
*
*	 Brick Breaker is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    Brick Breaker is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with Brick Breaker.  If not, see <http://www.gnu.org/licenses/>.
*/

//This "Board" class handles all game logic and displays items on the screen.

//Imports
import java.awt.*;
import javax.swing.*;
import java.util.Random;
import java.lang.Thread;
import javax.sound.sampled.*;
import java.io.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.TreeMap;
import java.awt.Toolkit.*;

//Class definition
public class Board extends JPanel implements Runnable, Constants {
	//Items on-screen
	private Paddle[] paddles;
	private Ball ball;
	private Brick[][] brick = new Brick[10][5];

	//Initial Values for some important variables
	private int score = 0, lives = MAX_LIVES, bricksLeft = MAX_BRICKS, waitTime = 3, xSpeed, level = 1;

	//Player's name
	private String playerName;

	private int networks = 200, inputs = 4, outputs = 2, deadCount = 0, fitness = 0;
	private Neat neat;
	
	
	//The game
	private Thread game;

	//Data structures to handle high scores
	private ArrayList<Item> items = new ArrayList<Item>();
	private AtomicBoolean isPaused = new AtomicBoolean(true);

	//Colors for the bricks
	private Color[] blueColors = {BLUE_BRICK_ONE, BLUE_BRICK_TWO, BLUE_BRICK_THREE, Color.BLACK};
	private Color[] redColors = {RED_BRICK_ONE, RED_BRICK_TWO, RED_BRICK_THREE, Color.BLACK};
	private Color[] purpleColors = {PURPLE_BRICK_ONE, PURPLE_BRICK_TWO, PURPLE_BRICK_THREE, Color.BLACK};
	private Color[] yellowColors = {YELLOW_BRICK_ONE, YELLOW_BRICK_TWO, YELLOW_BRICK_THREE, Color.BLACK};
	private Color[] pinkColors = {PINK_BRICK_ONE, PINK_BRICK_TWO, PINK_BRICK_THREE, Color.BLACK};
	private Color[] grayColors = {GRAY_BRICK_ONE, GRAY_BRICK_TWO, GRAY_BRICK_THREE, Color.BLACK};
	private Color[] greenColors = {GREEN_BRICK_ONE, GREEN_BRICK_TWO, GREEN_BRICK_THREE, Color.BLACK};
	private Color[][] colors = {blueColors, redColors, purpleColors, yellowColors, pinkColors, grayColors, greenColors};

	//Constructor
	public Board(int width, int height) {
		super.setSize(width, height);
		addKeyListener(new BoardListener());
		setFocusable(true);

		makeBricks();
		paddles = new Paddle[networks];
		for(int i = 0; i < networks; i++) {
			paddles[i] = new Paddle(PADDLE_X_START, PADDLE_Y_START, PADDLE_WIDTH, PADDLE_HEIGHT, Color.BLACK);
		}
		
		ball = new Ball(BALL_X_START, BALL_Y_START, BALL_WIDTH, BALL_HEIGHT, Color.BLACK);

		neat = new Neat(networks, inputs, outputs);
		
		game = new Thread(this);
		game.start();
	}

	//fills the array of bricks
	public void makeBricks() {
		for(int i = 0; i < 10; i++) {
			for(int j = 0; j < 5; j++) {
				Random rand = new Random();
				int itemType = rand.nextInt(3) + 1;
				int numLives = 3;
				Color color = colors[rand.nextInt(7)][0];
				brick[i][j] = new Brick((i * BRICK_WIDTH), ((j * BRICK_HEIGHT) + (BRICK_HEIGHT / 2)), BRICK_WIDTH - 5, BRICK_HEIGHT - 5, color, numLives, itemType);
			}
		}
	}

	//starts the thread
	public void start() {
		game.resume();
		isPaused.set(false);
	}

	//stops the thread
	public void stop() {
		game.suspend();
	} 

	//ends the thread
	public void destroy() {
		game.resume();
		isPaused.set(false);
		game.stop();
		isPaused.set(true);
	}

	//runs the game
	public void run() {
		xSpeed = 1;
		while(true) {
			int x1 = ball.getX();
			int y1 = ball.getY();

			//Makes sure speed doesnt get too fast/slow
			if (Math.abs(xSpeed) > 1) {
				if (xSpeed > 1) {
					xSpeed--;
				}
				if (xSpeed < 1) {
					xSpeed++;
				}
			}

			checkPaddle(x1, y1);
			checkWall(x1, y1);
			checkBricks(x1, y1);
			checkLives();
			checkIfOut(x1, y1);
			ball.move();
			netActivate(x1, y1);
			repaint();
			fitness++;	
			
			if(deadCount >= networks - 1) {
				neat.runNeat();
				ball.reset();
				fitness = 0;
				deadCount = 0;
				score = 0;
				makeBricks();
				for(Paddle p : paddles) {
					p.reset();
				}
			}
			
			try {
				game.sleep(waitTime);
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
		}
	}
	
	public void netActivate(int x1, int y1) {
		for(int i = 0; i < networks; i++) {
			if(!paddles[i].isDead) {
				float[] inputs = {paddles[i].getX(), paddles[i].getY(), x1, y1};
				
				neat.population[i].networkActivate(inputs);
				
				switch (neat.population[i].output) {
				case 0: paddles[i].setX(paddles[i].getX() + 2);
					break;
				case 1: paddles[i].setX(paddles[i].getX() - 2);
					break;
				case 2: 
					break;
				}
			}
		}
	}

	public void checkLives() {
		if (bricksLeft == NO_BRICKS) {
			ball.reset();
			bricksLeft = MAX_BRICKS;
			makeBricks();
			lives++;
			level++;
			score += 100;
			repaint();
		}
		if (lives == MIN_LIVES) {
			repaint();
			lives = MAX_LIVES;
			ball.reset();
			makeBricks();
			for(int i = 0; i < networks; i++) {
				paddles[i].reset();
			}
		}
	}

	public void checkPaddle(int x1, int y1) {
		for(int i = 0; i < networks; i++) {
			if (!paddles[i].isDead && paddles[i].hitPaddle(x1, y1) && ball.getXDir() < 0) {
				ball.setYDir(-1);
				xSpeed = -1;
				ball.setXDir(xSpeed);
			}
			if (!paddles[i].isDead && paddles[i].hitPaddle(x1, y1) && ball.getXDir() > 0) {
				ball.setYDir(-1);
				xSpeed = 1;
				ball.setXDir(xSpeed);
			}
	
			if (!paddles[i].isDead && paddles[i].getX() <= 0) {
				paddles[i].setX(0);
			}
			if (!paddles[i].isDead && paddles[i].getX() + paddles[i].getWidth() >= getWidth()) {
				paddles[i].setX(getWidth() - paddles[i].getWidth());
			}
		}
	}

	public void checkWall(int x1, int y1) {
		if (x1 >= getWidth() - ball.getWidth()) {
			xSpeed = -Math.abs(xSpeed);
			ball.setXDir(xSpeed);
		}
		if (x1 <= 0) {
			xSpeed = Math.abs(xSpeed);
			ball.setXDir(xSpeed);
		}
		if (y1 <= 0) {
			ball.setYDir(1);
		}
		if (y1 >= getHeight()) {
			ball.setYDir(-1);
		}
	}

	public void checkBricks(int x1, int y1) {
		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 5; j++) {
				if (brick[i][j].hitBottom(x1, y1)) {
					ball.setYDir(1);
					if (brick[i][j].isDestroyed()) {
						bricksLeft--;
						score += 50;
					}
				}
				if (brick[i][j].hitLeft(x1, y1)) {
					xSpeed = -xSpeed;
					ball.setXDir(xSpeed);
					if (brick[i][j].isDestroyed()) {
						bricksLeft--;
						score += 50;
					}
				}
				if (brick[i][j].hitRight(x1, y1)) {
					xSpeed = -xSpeed;
					ball.setXDir(xSpeed);
					if (brick[i][j].isDestroyed()) {
						bricksLeft--;
						score += 50;
					}
				}
				if (brick[i][j].hitTop(x1, y1)) {
					ball.setYDir(-1);
					if (brick[i][j].isDestroyed()) {
						bricksLeft--;
						score += 50;
					}
				}
			}
		}
	}

	public void checkIfOut(int x1, int y1) {
		for(int i = 0; i < networks; i++) {
			if (!paddles[i].isDead && y1 > PADDLE_Y_START - 10 && (x1 > paddles[i].getX() + PADDLE_WIDTH || x1 < paddles[i].getX())) {
				repaint();
				paddles[i].isDead = true;
				neat.population[i].setFitness(fitness);
				deadCount++;
			}
		}
	}

	//fills the board
	@Override
	public void paintComponent(Graphics g) {
		Toolkit.getDefaultToolkit().sync();
		super.paintComponent(g);
		ball.draw(g);
		
		for(Paddle p : paddles) {
			if(!p.isDead)
				p.draw(g);
		}
		
//        Graphics2D g2 = (Graphics2D) g;
//        Line2D lin = new Line2D.Float(PADDLE_X_START, 0, PADDLE_X_START, 1000);
//        g2.draw(lin);

		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 5; j++) {
				brick[i][j].draw(g);
			}
		}
		g.setColor(Color.BLACK);
		g.drawString("Alive: " + (networks - deadCount), 10, getHeight() - (getHeight()/10));
		g.drawString("Score: " + score, 10, getHeight() - (2*(getHeight()/10)) + 25);
		g.drawString("Level: " + level, 10, getHeight() - (3*(getHeight()/10)) + 50);
		//g.drawString("Player: " + playerName, 10, getHeight() - (4*(getHeight()/10)) + 75);

		for (Item i: items) {
			i.draw(g);
		}

		if (lives == MIN_LIVES) {
			
		}
	}

	//Private class that handles gameplay and controls
	private class BoardListener extends KeyAdapter {
	/*	@Override
		public void keyPressed(KeyEvent ke) {
			int key = ke.getKeyCode();
			if (key == KeyEvent.VK_LEFT) {
				paddle.setX(paddle.getX() - 50);
			}
			if (key == KeyEvent.VK_RIGHT) {
				paddle.setX(paddle.getX() + 50);
			}
		}
		@Override
		public void keyReleased(KeyEvent ke) {
			int key = ke.getKeyCode();
			if (key == KeyEvent.VK_LEFT) {
				paddle.setX(paddle.getX());
			}
			if (key == KeyEvent.VK_RIGHT) {
				paddle.setX(paddle.getX());
			}
		}*/
	}
}
