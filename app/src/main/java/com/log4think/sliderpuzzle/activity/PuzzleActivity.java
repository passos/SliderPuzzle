package com.log4think.sliderpuzzle.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.log4think.sliderpuzzle.R;
import com.log4think.sliderpuzzle.view.PuzzleView;

public class PuzzleActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_puzzle);

    PuzzleView view = (PuzzleView) findViewById(R.id.puzzleView);
    view.setBoardSize(4, 4);
    view.setCellPadding(1);
  }
}
