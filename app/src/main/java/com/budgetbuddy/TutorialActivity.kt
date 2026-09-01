package com.budgetbuddy

import android.os.Bundle

/** Compatibility entry point that opens the guided tour on the real app screens. */
/*
 * Start of class
 * Name of class and related classes (parent/child classes): TutorialActivity
 * Parent class: BaseActivity; child classes: none; related classes: TutorialFlow and BaseActivity.
 * What the class does: Hosts tutorial content that is separate from the live screen walkthrough.
 * What's important to other classes, if applicable: It must preserve BaseActivity appearance behavior and use LocalDataStore as the offline source of truth.
 * Code with comments begins below.
 */
class TutorialActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TutorialFlow.start(this)
    }

    companion object {
        const val EXTRA_REPLAY = "replayTutorial"
    }
}
// End of class: TutorialActivity
