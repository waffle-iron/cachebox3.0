/*
 * Copyright (C) 2016 team-cachebox.de
 *
 * Licensed under the : GNU General Public License (GPL);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.gnu.org/licenses/gpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.longri.cachebox3.gui.stages;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;
import de.longri.cachebox3.CB;
import de.longri.cachebox3.GlobalCore;
import de.longri.cachebox3.gui.actions.*;
import de.longri.cachebox3.gui.views.AboutView;
import de.longri.cachebox3.gui.views.AbstractView;
import de.longri.cachebox3.gui.widgets.ActionButton;
import de.longri.cachebox3.gui.widgets.ButtonBar;
import de.longri.cachebox3.gui.widgets.GestureButton;
import org.slf4j.LoggerFactory;

/**
 * Created by Longri on 20.07.2016.
 */
public class ViewManager extends Stage {

    final static org.slf4j.Logger log = LoggerFactory.getLogger(ViewManager.class);


    private AbstractView actView;
    private final float bottonsize, width, height;
    private final ButtonBar mainButtonBar;

    GestureButton db_button, cache_button, navButton, tool_button, misc_button;

    public ViewManager() {

        //set this to static CB for global access
        CB.viewmanager = this;

        bottonsize = CB.getScaledFloat(64);
        width = Gdx.graphics.getWidth();
        height = Gdx.graphics.getHeight();

        mainButtonBar = new ButtonBar(CB.getSkin().get("main_button_bar", ButtonBar.ButtonBarStyle.class),
                ButtonBar.Type.DISTRIBUTED);
        mainButtonBar.setBounds(0, 0, width, bottonsize);
        this.addActor(mainButtonBar);

        Gdx.app.log("ScaleFactor", Float.toString(CB.getScaledFloat(1)));
        Gdx.app.log("Width", Float.toString(Gdx.graphics.getWidth()));
        Gdx.app.log("Height", Float.toString(Gdx.graphics.getHeight()));
        Gdx.app.log("PPI", Float.toString(Gdx.graphics.getPpiX()));

        db_button = new GestureButton("db");
        db_button.setSize(bottonsize, bottonsize);
        db_button.addListener(gestureListener);
        mainButtonBar.addButton(db_button);

        cache_button = new GestureButton("cache");
        cache_button.setSize(bottonsize, bottonsize);
        cache_button.addListener(gestureListener);
        mainButtonBar.addButton(cache_button);


        navButton = new GestureButton("nav");
        navButton.setSize(bottonsize, bottonsize);
        navButton.addListener(gestureListener);
        mainButtonBar.addButton(navButton);


        tool_button = new GestureButton("tool");
        tool_button.setSize(bottonsize, bottonsize);
        tool_button.addListener(gestureListener);
        mainButtonBar.addButton(tool_button);

        misc_button = new GestureButton("misc");
        misc_button.setSize(bottonsize, bottonsize);
        misc_button.addListener(gestureListener);
        mainButtonBar.addButton(misc_button);

        mainButtonBar.layout();
        initialActionButtons();
        showView(new AboutView());
    }

    public void showView(AbstractView view) {

        log.debug("show view:" + view.getName());

        if (actView != null) {
            log.debug("remove and dispose actView" + actView.getName());
            this.getRoot().removeActor(actView);
            actView.saveState();
            actView.dispose();
        }

        this.actView = view;
        this.addActor(view);
        setActViewBounds();
        log.debug("reload view state:" + view.getName());
        this.actView.reloadState();
    }

    //TODO inital with longPressDuration from settings
    //    ActorGestureListener(float halfTapSquareSize, float tapCountInterval, float longPressDuration, float maxFlingDelay)
    ActorGestureListener gestureListener = new ActorGestureListener() {

        @Override
        public void fling(InputEvent event, float velocityX, float velocityY, int button) {
            //TODO add gesture detection
        }

        /**
         * If true is returned, additional gestures will not be triggered.
         * @param actor
         * @param x
         * @param y
         * @return
         */
        @Override
        public boolean longPress(Actor actor, float x, float y) {
            Gdx.app.log("", "Longpress");
            return true;
        }

        @Override
        public void tap(InputEvent event, float x, float y, int count, int button) {
            if (event.getType() == InputEvent.Type.touchUp) {

                GestureButton target = (GestureButton) event.getTarget();

                for (Button b : mainButtonBar.getButtons()) {
                    GestureButton gb = (GestureButton) b;
                    if (gb.equals(target)) {
                        gb.setChecked(true);
                        gb.executeDefaultAction();
                    } else {
                        gb.setChecked(false);
                    }
                }

            }
        }

    };


    private void initialActionButtons() {
        // assign the actions to the buttons

        db_button.addAction(new ActionButton(new Action_Show_CacheList(), true, ActionButton.GestureDirection.Up));
        db_button.addAction(new ActionButton(new Action_Show_TrackableListView(), false, ActionButton.GestureDirection.Right));
        db_button.addAction(new ActionButton(new Action_Show_TrackListView(), false, ActionButton.GestureDirection.Down));

//        mDescriptionButtonOnLeftTab.addAction(new CB_ActionButton(actionShowDescriptionView, true, GestureDirection.Up));
//        mDescriptionButtonOnLeftTab.addAction(new CB_ActionButton(actionShowWaypointView, false, GestureDirection.Right));
//        mDescriptionButtonOnLeftTab.addAction(new CB_ActionButton(actionShowLogView, false, GestureDirection.Down));
//        mDescriptionButtonOnLeftTab.addAction(new CB_ActionButton(actionShowHint, false));
//        mDescriptionButtonOnLeftTab.addAction(new CB_ActionButton(actionShowDescExt, false));
//        mDescriptionButtonOnLeftTab.addAction(new CB_ActionButton(actionShowSpoilerView, false));
//        mDescriptionButtonOnLeftTab.addAction(new CB_ActionButton(actionShowNotesView, false));
//
//        mMapButtonOnLeftTab.addAction(new CB_ActionButton(actionShowMap, true, GestureDirection.Up));
//        mMapButtonOnLeftTab.addAction(new CB_ActionButton(actionShowCompassView, false, GestureDirection.Right));
//        mMapButtonOnLeftTab.addAction(new CB_ActionButton(actionNavigateTo1, false, GestureDirection.Down));
//        mMapButtonOnLeftTab.addAction(new CB_ActionButton(actionGenerateRoute, false, GestureDirection.Left));
        if (GlobalCore.isTestVersion())
            navButton.addAction(new ActionButton(new Action_Show_TestView(), false));
//
//        mToolsButtonOnLeftTab.addAction(new CB_ActionButton(actionQuickFieldNote, false, GestureDirection.Up));
//        mToolsButtonOnLeftTab.addAction(new CB_ActionButton(actionShowFieldNotesView, Config.ShowFieldnotesAsDefaultView.getValue()));
//        mToolsButtonOnLeftTab.addAction(new CB_ActionButton(actionRecTrack, false));
//        mToolsButtonOnLeftTab.addAction(new CB_ActionButton(actionRecVoice, false));
//        mToolsButtonOnLeftTab.addAction(new CB_ActionButton(actionRecPicture, false, GestureDirection.Down));
//        mToolsButtonOnLeftTab.addAction(new CB_ActionButton(actionRecVideo, false));
//        mToolsButtonOnLeftTab.addAction(new CB_ActionButton(actionParking, false));
//        mToolsButtonOnLeftTab.addAction(new CB_ActionButton(actionShowSolverView, false, GestureDirection.Left));
//        mToolsButtonOnLeftTab.addAction(new CB_ActionButton(actionShowSolverView2, false, GestureDirection.Right));
        tool_button.addAction(new ActionButton(new Action_Show_Quit(), true));
//
        misc_button.addAction(new ActionButton(new Action_Show_AboutView(), true, ActionButton.GestureDirection.Up));
//        mAboutButtonOnLeftTab.addAction(new CB_ActionButton(actionShowCreditsView, false));
//        mAboutButtonOnLeftTab.addAction(new CB_ActionButton(actionShowSettings, false, GestureDirection.Left));
//        mAboutButtonOnLeftTab.addAction(new CB_ActionButton(actionDayNight, false));
//        mAboutButtonOnLeftTab.addAction(new CB_ActionButton(actionHelp, false));
//        mAboutButtonOnLeftTab.addAction(new CB_ActionButton(actionClose, false, GestureDirection.Down));
//
//        actionShowAboutView.Execute();
    }


    private void setActViewBounds() {
        this.actView.setBounds(0, bottonsize, width, height);
    }

    public AbstractView getActView() {
        return actView;
    }

}
