package com.sonnybell.app.games;

import com.sonnybell.app.client.ClientHandler;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Capital Game is a multiplayer game that involves players answering questions
 * about world capitals.
 * Any connected player can answer the current question, and the game keeps
 * track of scores.
 */
public final class CapitalGame {

    // Winning score for the game
    private static final int WINNING_SCORE = 5;

    // Time constants for game delays
    // 0.5 seconds
    private static final int HALF_SECOND = 500;

    // 1 second
    private static final int ONE_SECOND = 1000;

    // 2 seconds
    private static final int TWO_SECONDS = 2000;

    // 3 seconds
    private static final int THREE_SECONDS = 3000;

    // 30 seconds
    private static final long QUESTION_TIMEOUT = 30000;

    // Static final collections
    private static final List<String> QUESTIONS = new CopyOnWriteArrayList<>();
    private static final List<String> ANSWERS = new CopyOnWriteArrayList<>();
    private static final Map<String, Integer> PLAYER_SCORES = new ConcurrentHashMap<>();

    // Static variables
    private static String currentQuestion = "";
    private static String currentAnswer = "";
    private static boolean gameActive;
    private static int currentQuestionIndex = -1;

    // Static initializer block
    static {
        initializeQuestionsAndAnswers();
    }

    // Private constructor to prevent instantiation
    private CapitalGame() {
        // Utility class
    }

    private static void initializeQuestionsAndAnswers() {
        // Questions
        QUESTIONS.add("What is the capital of France?");
        QUESTIONS.add("What is the capital of Japan?");
        QUESTIONS.add("What is the capital of Brazil?");
        QUESTIONS.add("What is the capital of Canada?");
        QUESTIONS.add("What is the capital of Australia?");
        QUESTIONS.add("What is the capital of Germany?");
        QUESTIONS.add("What is the capital of Egypt?");
        QUESTIONS.add("What is the capital of India?");
        QUESTIONS.add("What is the capital of Russia?");
        QUESTIONS.add("What is the capital of South Africa?");
        QUESTIONS.add("What is the capital of Italy?");
        QUESTIONS.add("What is the capital of China?");
        QUESTIONS.add("What is the capital of Mexico?");
        QUESTIONS.add("What is the capital of Argentina?");
        QUESTIONS.add("What is the capital of South Korea?");
        QUESTIONS.add("What is the capital of Spain?");
        QUESTIONS.add("What is the capital of United Kingdom?");
        QUESTIONS.add("What is the capital of United States?");
        QUESTIONS.add("What is the capital of Saudi Arabia?");
        QUESTIONS.add("What is the capital of Turkey?");

        // Corresponding answers
        ANSWERS.add("Paris");
        ANSWERS.add("Tokyo");
        ANSWERS.add("Brasília");
        ANSWERS.add("Ottawa");
        ANSWERS.add("Canberra");
        ANSWERS.add("Berlin");
        ANSWERS.add("Cairo");
        ANSWERS.add("New Delhi");
        ANSWERS.add("Moscow");
        ANSWERS.add("Pretoria");
        ANSWERS.add("Rome");
        ANSWERS.add("Beijing");
        ANSWERS.add("Mexico City");
        ANSWERS.add("Buenos Aires");
        ANSWERS.add("Seoul");
        ANSWERS.add("Madrid");
        ANSWERS.add("London");
        ANSWERS.add("Washington DC");
        ANSWERS.add("Riyadh");
        ANSWERS.add("Ankara");
    }

    /**
     * Start a new capital game.
     */
    public static void startGame() {
        if (gameActive) {
            ClientHandler.broadcastMessageToAll("GAME: A game is already in progress!");
            return;
        }

        gameActive = true;
        PLAYER_SCORES.clear();

        // Send game start message with delays between instructions
        ClientHandler.broadcastMessageToAll("CAPITAL GAME STARTED! ");

        // Delay before next instruction
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                ClientHandler.broadcastMessageToAll("GAME: First to " + WINNING_SCORE + " correct answers wins!");
            }
            // Delay before next instruction
        }, ONE_SECOND);

        // Delay before next instruction
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                ClientHandler.broadcastMessageToAll("GAME: Type your answer in the chat to participate!");
            }
            // 2 seconds delay
        }, TWO_SECONDS);

        // Delay before first question
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                nextQuestion();
            }
            // Start the first question after a delay
        }, THREE_SECONDS);
    }

    /**
     * Stop the current game.
     */
    public static void stopGame() {
        if (!gameActive) {
            ClientHandler.broadcastMessageToAll("GAME: No game is currently running!");
            return;
        }

        // Reset game state
        gameActive = false;
        ClientHandler.broadcastMessageToAll("GAME STOPPED! ");

        // Small delay before showing scores
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                showScores();
            }
            // 0.5 second delay
        }, HALF_SECOND);
    }

    /**
     * Move to the next question.
     */
    private static void nextQuestion() {
        if (!gameActive) {
            return;
        }
        currentQuestionIndex = (int) (Math.random() * QUESTIONS.size());
        currentQuestion = QUESTIONS.get(currentQuestionIndex);
        currentAnswer = ANSWERS.get(currentQuestionIndex);
        // seconds for division
        final int second = 1000;

        ClientHandler.broadcastMessageToAll("QUESTION: " + currentQuestion);

        // Small delay before timeout message
        Timer instructionTimer = new Timer();
        instructionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                ClientHandler
                        .broadcastMessageToAll("GAME: You have " + (QUESTION_TIMEOUT / second) + " seconds to answer!");
            }
            // 0.5 second delay
        }, HALF_SECOND);

        // Schedule timeout for question
        Timer timeoutTimer = new Timer();
        timeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (gameActive && currentQuestion.equals(QUESTIONS.get(currentQuestionIndex))) {
                    ClientHandler.broadcastMessageToAll("TIME'S UP! The answer was: " + currentAnswer);

                    // Small delay before next question
                    Timer nextQuestionTimer = new Timer();
                    nextQuestionTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            nextQuestion();
                        }
                        // 1 second delay before next question
                    }, ONE_SECOND);
                }
            }
        }, QUESTION_TIMEOUT);
    }

    /**
     * Check if a player's message is an answer to the current question.
     */
    public static boolean checkAnswer(String username, String message) {
        if (!gameActive || currentAnswer.isEmpty()) {
            return false;
        }

        // starting score for players
        final int defaultScore = 0;
        // Add one score if correct answer
        final int correctAnswerScore = 1;

        // Check if the message is a correct answer (case-insensitive)
        if (message.trim().equalsIgnoreCase(currentAnswer)) {
            // Player got it right logic
            PLAYER_SCORES.put(username, PLAYER_SCORES.getOrDefault(username, defaultScore) + correctAnswerScore);
            int playerScore = PLAYER_SCORES.get(username);

            ClientHandler.broadcastMessageToAll("CORRECT! " + username + " got it right!");
            ClientHandler.broadcastMessageToAll("GAME: " + username + " now has " + playerScore + " point(s)!");

            // Check if player won
            if (playerScore >= WINNING_SCORE) {
                gameActive = false;
                ClientHandler.broadcastMessageToAll("GAME OVER! " + username + " WINS! ");
                showScores();
                return true;
            }
            // 2 seconds delay before next question
            final int delayBeforeNextQuestion = 2000;

            // Move to next question
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    nextQuestion();
                }
            }, delayBeforeNextQuestion);

            return true;
        }

        return false;
    }

    /**
     * Show current scores.
     */
    public static void showScores() {
        if (PLAYER_SCORES.isEmpty()) {
            ClientHandler.broadcastMessageToAll("GAME: No scores yet!");
            return;
        }

        ClientHandler.broadcastMessageToAll("CURRENT SCORES:");
        PLAYER_SCORES.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> ClientHandler
                        .broadcastMessageToAll("GAME: " + entry.getKey() + ": " + entry.getValue() + " point(s)"));
    }

    /**
     * Check if game is currently active.
     */
    public static boolean isGameActive() {
        return gameActive;
    }

    /**
     * Get game status.
     */
    public static String getGameStatus() {
        if (!gameActive) {
            return "No game is currently running. Type '/startgame' to start!";
        }
        return "Game in progress! Current question: " + currentQuestion;
    }
}
