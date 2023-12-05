package xzot1k.android.asg3;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ThreadLocalRandom;

public class MainActivity extends AppCompatActivity {

    private static MainActivity instance; // instance to retrieve in particular
    private File saveFile; // save file
    private long played, wins, losses; // data variables
    private boolean firstStart = false; // value to ensure the file is not loaded more than once

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        instance = this; // set global instance

        saveFile = new File(getFilesDir(), "data.json"); // initialize the save file

        // ensure this is the first time the app was started
        if (!firstStart) {
            firstStart = true;
            reset(false); // reset, in this case initialize, the data variables
            if (getSaveFile().exists()) load(); // check if the save file exists, if so, load all data values stored
        }


        // setup content view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        updateStats(); // update the stats on screen

        // setup the rock selection button and its listener
        ImageView rockButton = (ImageView) findViewById(R.id.rock);
        rockButton.setOnClickListener(view -> update(ThrowType.ROCK));

        // setup the paper selection button and its listener
        ImageView paperButton = (ImageView) findViewById(R.id.paper);
        paperButton.setOnClickListener(view -> update(ThrowType.PAPER));

        // setup the scissors selection button and its listener
        ImageView scissorsButton = (ImageView) findViewById(R.id.scissors);
        scissorsButton.setOnClickListener(view -> update(ThrowType.SCISSORS));

        // setup the reset button and its listener
        Button resetButton = (Button) findViewById(R.id.reset);
        resetButton.setOnClickListener(view -> {
            reset(true); // reset file values
            updateStats();          // update stats to current
        });

        save(); // save the file
    }

    /**
     * Update the entire UI based on player selection.
     *
     * @param playerThrowType What the image choose to throw.
     */
    public void update(ThrowType playerThrowType) {

        // get the player selection images
        ImageView rockImage = findViewById(R.id.rock),
                paperImage = findViewById(R.id.paper),
                scissorsImage = findViewById(R.id.scissors);

        // hide each image/button
        rockImage.setVisibility(View.INVISIBLE);
        rockImage.setEnabled(false);
        paperImage.setVisibility(View.INVISIBLE);
        paperImage.setEnabled(false);
        scissorsImage.setVisibility(View.INVISIBLE);
        scissorsImage.setEnabled(false);


        // hide the turn/move text
        TextView moveText = findViewById(R.id.moveText);
        moveText.setVisibility(View.INVISIBLE);


        Handler handler = new Handler(); // new handler for delays
        ThrowType aiThrowType = generateAIMove();  // the AI cast image to the image of the generate throw type

        playThrowAnimation(handler, playerThrowType, aiThrowType); // play the animation before revealing results

        // delay for suspense before revealing the AI's move
        handler.postDelayed(() -> {

            TextView winText = findViewById(R.id.resultText);  // get the result text (should be hidden at this point)

            // player win (checks possible win combos)
            if ((aiThrowType == ThrowType.ROCK && playerThrowType == ThrowType.PAPER)
                    || (aiThrowType == ThrowType.PAPER && playerThrowType == ThrowType.SCISSORS)
                    || (aiThrowType == ThrowType.SCISSORS && playerThrowType == ThrowType.ROCK)) {

                setWins(getWins() + 1);                          // update player win counter
                winText.setText(getString(R.string.playerWin));  // set the result text to player wins

            } // check if a TIE occurred
            else if ((aiThrowType == ThrowType.ROCK && playerThrowType == ThrowType.ROCK)
                    || (aiThrowType == ThrowType.SCISSORS && playerThrowType == ThrowType.SCISSORS)
                    || (aiThrowType == ThrowType.PAPER && playerThrowType == ThrowType.PAPER)) {

                winText.setText(getString(R.string.tie));  // set the result text to tie

            } else { // player loss

                setLosses(getLosses() + 1);                  // update player loss counter
                winText.setText(getString(R.string.aiWin));  // set the result text to AI wins

            }

            winText.setVisibility(View.VISIBLE);  // show result text (whether player or AI won)
            setPlayed(getPlayed() + 1);           // update played counter

            updateStats(); // update player's new statistics
            save();        // save to file (since not a lot is being saved, this doesn't need to be Async)

            handler.postDelayed(() -> {

                // hide the result text
                winText.setVisibility(View.INVISIBLE);

                // re-enable player throw buttons
                rockImage.setVisibility(View.VISIBLE);
                rockImage.setEnabled(true);

                paperImage.setVisibility(View.VISIBLE);
                paperImage.setEnabled(true);

                scissorsImage.setVisibility(View.VISIBLE);
                scissorsImage.setEnabled(true);

                // re-enable the move text and set text to state its the player's turn
                moveText.setText(getString(R.string.playerMove));
                moveText.setVisibility(View.VISIBLE);

            }, 2000);

        }, 7000); // 7s delay

    }

    /**
     * Play the rock, paper, scissors, shoot animation.
     *
     * @param handler         The handler for delays.
     * @param playerThrowType The selection the player made.
     * @param aiThrowType     The selection the AI made.
     */
    public void playThrowAnimation(Handler handler, ThrowType playerThrowType, ThrowType aiThrowType) {

        // the image and text to animate
        ImageView shootImage = findViewById(R.id.shootImage);
        TextView shootText = findViewById(R.id.shootText);

        // animation to be applied to the image
        Animation rockAnimation = AnimationUtils.loadAnimation(this, R.anim.rock_animation);

        int delay = 500;

        // run the animation for rock, paper, scissors & increment the delay by 1.2 seconds starting at 0.5 seconds
        animationNext(handler, shootImage, shootText, rockAnimation, ThrowType.ROCK, delay);
        animationNext(handler, shootImage, shootText, rockAnimation, ThrowType.PAPER, (delay += 1200));
        animationNext(handler, shootImage, shootText, rockAnimation, ThrowType.SCISSORS, (delay += 1200));

        // after approximately 4.5 seconds show results followed by who won & a statistic update
        handler.postDelayed(() -> {

            // the image views for what the AI & player threw
            ImageView aiCastImage = findViewById(R.id.aiCastImage),
                    playerCastImage = findViewById(R.id.playerCastImage);

            // the text views for what the AI & player threw
            TextView playerCastText = findViewById(R.id.playerCastText),
                    aiCastText = findViewById(R.id.aiCastText);

            // reset the shoot image
            shootImage.setImageDrawable(ThrowType.ROCK.getDrawable());
            shootImage.setTag(ThrowType.ROCK.getDrawable());
            shootImage.setVisibility(View.INVISIBLE);

            // reset the shoot text
            shootText.setText(ThrowType.ROCK.getDisplayName());
            shootText.setVisibility(View.INVISIBLE);

            // update player cast/throw image and text
            playerCastImage.setImageDrawable(playerThrowType.getDrawable());
            playerCastImage.setTag(playerThrowType.getDrawable());
            playerCastImage.setVisibility(View.VISIBLE);
            playerCastText.setText(getString(R.string.playerThrow).replace("{throw}", playerThrowType.getDisplayName()));
            playerCastText.setVisibility(View.VISIBLE);

            // update AI cast/throw image and text
            aiCastImage.setImageDrawable(aiThrowType.getDrawable());
            aiCastImage.setTag(aiThrowType.getDrawable());
            aiCastImage.setVisibility(View.VISIBLE);
            aiCastText.setText(getString(R.string.aiThrow).replace("{throw}", aiThrowType.getDisplayName()));
            aiCastText.setVisibility(View.VISIBLE);

            // after 2 seconds hide the thrown image and text for the AI & player
            handler.postDelayed(() -> {
                playerCastImage.setVisibility(View.INVISIBLE);
                playerCastText.setVisibility(View.INVISIBLE);
                aiCastImage.setVisibility(View.INVISIBLE);
                aiCastText.setVisibility(View.INVISIBLE);
            }, 2000);

        }, (delay += 1600));
    }

    /**
     * Helper function to reduce repeated code.
     *
     * @param handler       Handler for delays.
     * @param shootImage    The image to update.
     * @param shootText     The text to update.
     * @param rockAnimation The animation to apply to the image.
     * @param nextType      The new type to set as the image.
     * @param delay         The delay before changes are applyed.
     */
    public void animationNext(Handler handler, ImageView shootImage, TextView shootText, Animation rockAnimation, ThrowType nextType, int delay) {
        handler.postDelayed(() -> {
            shootImage.setImageDrawable(nextType.getDrawable());
            shootImage.setTag(nextType.getDrawable());
            shootImage.setVisibility(View.VISIBLE);

            shootImage.startAnimation(rockAnimation);

            shootText.setText(nextType.getDisplayName());
            shootText.setVisibility(View.VISIBLE);
        }, delay);
    }

    /**
     * @return The randomly selected move for the AI to throw.
     */
    public ThrowType generateAIMove() {
        // generate a random selection in the ThrowType enumeration
        int randomIndex = (ThreadLocalRandom.current().nextInt(ThrowType.values().length) % ThrowType.values().length);
        return ThrowType.values()[randomIndex]; // get that random selection and return it
    }

    /**
     * Update the stats text to show current values.
     */
    private void updateStats() {

        final int rate = (int) (getPlayed() == 0 ? 0 : (((double) getWins()) / ((double) getPlayed())) * 100); // calculate win rate

        // update stats text view (replace all placeholders with stats)
        TextView stats = (TextView) findViewById(R.id.stats);
        stats.setText(getString(R.string.stats)
                .replace("{played}", String.valueOf(getPlayed()))
                .replace("{wins}", String.valueOf(getWins()))
                .replace("{losses}", String.valueOf(getLosses()))
                .replace("{rate}", String.valueOf(rate)));
    }

    /**
     * Resets the save file.
     *
     * @param resetSaveFile Whether to save the reset values to the file.
     */
    public void reset(boolean resetSaveFile) {

        // initialize all stats
        setPlayed(0);
        setWins(0);
        setLosses(0);

        if (resetSaveFile) save(); // if reset, reset the save file by updatting values
    }

    /**
     * Loads all saved data from the save file.
     */
    public void load() {

        if (!getSaveFile().exists()) return; // check if the file exists, return if not

        // open new buffered reader that auto closes
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(openFileInput(getSaveFile().getName())))) {

            StringBuilder contentString = new StringBuilder(); // content string builder for the json

            String line; // current line bytes
            do {

                line = bufferedReader.readLine();   // read next line
                contentString.append(line);         // append line to content string

            } while (line != null); // loop until the line is invalid

            JSONObject dataObject = new JSONObject(contentString.toString()); // new json object given the content string

            // load all data values into memory
            setPlayed(dataObject.getLong("played"));
            setWins(dataObject.getLong("wins"));
            setLosses(dataObject.getLong("losses"));

        } catch (JSONException | IOException e) {e.printStackTrace();} // catch exceptions and print error to console
    }

    /**
     * Save all data to the save file.
     */
    public void save() {
        try {
            JSONObject dataObject = new JSONObject(); // new json object to save data

            // set data values into the json object
            dataObject.put("played", getPlayed());
            dataObject.put("wins", getWins());
            dataObject.put("losses", getLosses());

            FileWriter fileWriter = new FileWriter(getSaveFile());          // new file writer for the save file
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter); // new buffered writer to write to the save file writer
            bufferedWriter.write(dataObject.toString());                    // write json object string to the file

            // close both the buffered and file writers
            bufferedWriter.close();
            fileWriter.close();

        } catch (JSONException | IOException e) {e.printStackTrace();} // catch exceptions and print error to console
    }

    // getters & setters
    public static MainActivity getInstance() {return instance;}

    public File getSaveFile() {return saveFile;}

    public long getPlayed() {return played;}

    public void setPlayed(long played) {this.played = played;}

    public long getWins() {return wins;}

    public void setWins(long wins) {this.wins = wins;}

    public long getLosses() {return losses;}

    public void setLosses(long losses) {this.losses = losses;}

    public enum ThrowType {
        ROCK(R.drawable.rock), PAPER(R.drawable.paper), SCISSORS(R.drawable.scissors);

        private final Drawable drawable;  // the drawable associated to the type
        private final String displayName; // display name of the type

        ThrowType(int id) {
            // set the drawable image to that of the id
            drawable = ContextCompat.getDrawable(MainActivity.getInstance().getApplicationContext(), id);

            // set the display name to the respected name with only the first letter capitalized
            displayName = (Character.toUpperCase(name().toLowerCase().charAt(0)) + name().toLowerCase().substring(1));
        }

        public Drawable getDrawable() {return drawable;}

        public String getDisplayName() {return displayName;}

    }

}