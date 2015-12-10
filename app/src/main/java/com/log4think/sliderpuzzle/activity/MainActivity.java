package com.log4think.sliderpuzzle.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.log4think.sliderpuzzle.R;
import com.log4think.sliderpuzzle.view.BoardView;

public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    BoardView boardView = (BoardView) findViewById(R.id.puzzleView);
    boardView.setBoardSize(4, 4);
    boardView.setCellPadding(1);
  }
}
