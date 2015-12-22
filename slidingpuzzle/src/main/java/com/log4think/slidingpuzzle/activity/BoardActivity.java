package com.log4think.slidingpuzzle.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.log4think.slidingpuzzle.R;
import com.log4think.slidingpuzzle.view.BoardView;

public class BoardActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_board);

    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayShowHomeEnabled(true);
      actionBar.setIcon(R.mipmap.ic_menu_white_24dp);
    }

    final BoardView boardView = (BoardView) findViewById(R.id.boardView);
    boardView.setBoardSize(4, 4);
    boardView.setCellPadding(1);

    findViewById(R.id.shuffle_button).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        boardView.shuffle();
      }
    });

    findViewById(R.id.restore_button).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        boardView.restore();
      }
    });

    final TextView headerTitle = (TextView) findViewById(R.id.headerTitleView);
    boardView.setOnMovedListener(new BoardView.OnMovedListener() {
      @Override
      public void onMoved() {
        headerTitle.setText(String.format("%03d", boardView.getPuzzleSteps()));
      }
    });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();

    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }
}
