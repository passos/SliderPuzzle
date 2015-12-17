package com.log4think.sliderpuzzle.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.log4think.sliderpuzzle.R;
import com.log4think.sliderpuzzle.view.BoardView;

public class BoardActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_board);

    BoardView view = (BoardView) findViewById(R.id.boardView);
    view.setBoardSize(4, 4);
    view.setCellPadding(1);
  }
}
