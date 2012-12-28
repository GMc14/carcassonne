package net.client;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import model.GameState;
import model.PlayerStruct;
import ui.GameUi;

// Adapter class which receives the returned messages from the server.
// The received messages are processed, followed by the client being told to
// update itself.
public class ClientProtocol extends SocketClientProtocol {

	private GameUi gameUi = null;

	public ClientProtocol(GameUi gameUi) {
		this.gameUi = gameUi;
	}

	// Variables to keep track of scoring process; after all the player's scores
	// are updated then we can end the current players turn if they don't have
	// any meeples left.
	private int numPlayerScoresUpdated = 0;
	private boolean currentPlayerHasMeeplesLeft = true;

	@Override
	public ArrayList<String> processInput(Socket sender, String input) {

		// Convert the input string into a list for processing.
		List<String> message = Arrays.asList(input.split(";"));

		// PRE-GAME

		if (message.get(0).equals(SocketClientProtocol.EXIT)) {
			gameUi.exit();
		}

		// ASSIGNPLAYER;player;<int>
		if (message.get(0).equals("ASSIGNPLAYER")) {

			int playerId = Integer.parseInt(message.get(2));

			gameUi.setPlayer(playerId);
		}

		// UPDATELOBBY[;player;<int>;name;<string>;color;<string:(RGB)>]+
		if (message.get(0).equals("UPDATELOBBY")) {

			HashMap<Integer, PlayerStruct> players;
			players = new HashMap<Integer, PlayerStruct>();

			// Parse the message, and place each player in a map to pass on.
			for (int i = 1; i < message.size(); i = i + 6) {

				int rep = Integer.parseInt(message.get(i + 1));
				String name = message.get(i + 3);
				String color = message.get(i + 5);

				players.put(rep, new PlayerStruct(name, color));
			}

			gameUi.updateLobby(players);
		}

		// In-game messages.

		// INIT;currentPlayer;<int>;gameBoardWidth;<int>;gameBoardHeight;<int>
		if (message.get(0).equals("INIT")) {

			int currentPlayer = Integer.parseInt(message.get(2));
			int width = Integer.parseInt(message.get(4));
			int height = Integer.parseInt(message.get(6));

			gameUi.startGame(currentPlayer, width, height);
		}

		// DRAWTILE;currentPlayer;<int>;identifier;<string>;orientation;<int:[0-3]>
		if (message.get(0).equals("DRAWTILE")) {

			int currentPlayer = Integer.parseInt(message.get(2));
			String identifier = message.get(4);
			int orientation = Integer.parseInt(message.get(6));

			gameUi.drawTile(currentPlayer, identifier, orientation);
		}

		// ROTATETILE;currentPlayer;<int>;direction;<string:(clockwise|counterClockwise)>
		if (message.get(0).equals("ROTATETILE")) {

			int currentPlayer = Integer.parseInt(message.get(2));
			String direction = message.get(4);

			gameUi.rotateTile(currentPlayer, direction);
		}

		// PLACETILE;currentPlayer;<int>;xBoard;<int>;yBoard;<int>;error;<int:(0|1)>
		if (message.get(0).equals("PLACETILE")) {

			int currentPlayer = Integer.parseInt(message.get(2));
			int xBoard = Integer.parseInt(message.get(4));
			int yBoard = Integer.parseInt(message.get(6));
			int err = Integer.parseInt(message.get(8));

			gameUi.placeTile(currentPlayer, xBoard, yBoard, err);
		}

		// PLACEMEEPLE;currentPlayer;<int>;xBoard;<int>;yBoard;<int>;xTile;<int>;yTile;<int>;error;<int:(0|1)>
		if (message.get(0).equals("PLACEMEEPLE")) {

			int currentPlayer = Integer.parseInt(message.get(2));
			int xBoard = Integer.parseInt(message.get(4));
			int yBoard = Integer.parseInt(message.get(6));
			int xTile = Integer.parseInt(message.get(8));
			int yTile = Integer.parseInt(message.get(10));
			int err = Integer.parseInt(message.get(12));

			gameUi.placeMeeple(currentPlayer, xBoard, yBoard, xTile, yTile, err);
		}

		if (gameUi.getGameState() == GameState.SCORE_PLACE_TILE
				|| gameUi.getGameState() == GameState.SCORE_PLACE_MEEPLE) {

			// Remove meeples.
			if (message.get(0).equals("SCORE")) {
				gameUi.scoreRemoveMeeples(message);
			}

			// Update player info.
			if (message.get(0).equals("INFO")
					&& message.get(1).equals("player")) {

				int player = 0;
				int playerScore = 0;
				int meeplesPlaced = 0;

				if (message.get(1).equals("player")) {
					player = Integer.parseInt(message.get(2));
				}
				if (message.get(5).equals("score")) {
					playerScore = Integer.parseInt(message.get(6));
				}
				if (message.get(7).equals("meeplesPlaced")) {
					meeplesPlaced = Integer.parseInt(message.get(8));
				}

				gameUi.getPlayerStatusPanels().get(player)
						.setScore(playerScore);

				// Each players info is sent after a scoring action;
				// after scoring all players, if the current player has no
				// meeples to place then end their turn.
				numPlayerScoresUpdated++;

				if (meeplesPlaced == 7 && gameUi.getPlayer() == player) {
					currentPlayerHasMeeplesLeft = false;
				}

				if (numPlayerScoresUpdated == gameUi.getNumPlayers()) {

					numPlayerScoresUpdated = 0;

					// Also end the player's turn if they are at the meeple
					// scoring state.
					if (gameUi.getGameState() == GameState.SCORE_PLACE_MEEPLE
							|| !currentPlayerHasMeeplesLeft) {

						currentPlayerHasMeeplesLeft = true;

						String msg = "ENDTURN;currentPlayer;" + player;
						gameUi.sendMessage(msg);

						gameUi.updateGameState(GameState.DRAW_TILE);
						gameUi.endTurn(gameUi.getCurrentPlayer());

					} else {

						gameUi.updateGameState(GameState.PLACE_MEEPLE);
						gameUi.getEndTurnButton().setEnabled(true);
					}
				}
			}
		}

		if (message.get(0).equals("ENDTURN")) {

			gameUi.updateGameState(GameState.DRAW_TILE);
			gameUi.endTurn(Integer.parseInt(message.get(2)));
		}

		// Info
		if (message.get(0).equals("INFO") && message.get(1).equals("game")) {

			boolean drawPileEmpty = false;

			if (message.get(4).equals("drawPileEmpty")) {

				int isDrawPileEmpty = Integer.parseInt(message.get(5));
				drawPileEmpty = (isDrawPileEmpty == 0) ? false : true;
			}

			if (drawPileEmpty) {

				gameUi.updateGameState(GameState.END_GAME);
			}
		}

		return null;
	}
}
