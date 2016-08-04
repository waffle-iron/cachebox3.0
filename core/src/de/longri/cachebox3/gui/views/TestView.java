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
package de.longri.cachebox3.gui.views;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.StringBuilder;
import com.kotcrab.vis.ui.widget.VisDialog;
import com.kotcrab.vis.ui.widget.VisLabel;
import de.longri.cachebox3.CB;
import de.longri.cachebox3.gui.widgets.ColorDrawable;
import de.longri.cachebox3.gui.widgets.ColorWidget;
import de.longri.cachebox3.settings.Config;
import de.longri.cachebox3.utils.HSV_Color;
import org.slf4j.LoggerFactory;

/**
 * Created by Longri on 27.07.16.
 */
public class TestView extends AbstractView {
    final static org.slf4j.Logger log = LoggerFactory.getLogger(TestView.class);

    public TestView() {
        super("TestView");
    }

    protected void create() {
        // create a Label with name for default
        nameLabel = new VisLabel(this.name);
        nameLabel.setAlignment(Align.center);
        nameLabel.setPosition(10, 10);
        nameLabel.setWrap(true);

        colorWidget = new ColorWidget(CB.getSkin().getColor("abstract_background"));
        colorWidget.setBounds(0, 0, this.getWidth(), this.getHeight());

        this.addActor(colorWidget);
        this.addActor(nameLabel);

        Button testButton = new Button(CB.getSkin());
        testButton.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {

                VisDialog dialog = new VisDialog("Warning", "dialog") {
                    public void result(Object obj) {
                        System.out.println("result " + obj);
                        this.hide();
                        this.remove();
                    }
                };

                HSV_Color color = new HSV_Color(HSV_Color.FIREBRICK);
                color.a = 0.5f;
                color.clamp();

//                dialog.getStyle().stageBackground = new ColorDrawable(color);
                dialog.text("Are you sure you want to quit?");
                dialog.button("Yes", true); //sends "true" as the result
                dialog.button("No", false);  //sends "false" as the result
                // dialog.key(Input.Keys.ENTER, true); //sends "true" when the ENTER key is pressed
                dialog.show(CB.viewmanager);


            }
        });
        this.addActor(testButton);

    }


    @Override
    public void reloadState() {


        StringBuilder sb = new StringBuilder();

        sb.append("LaunchCount:" + Config.AppRaterlaunchCount.getValue());

        nameLabel.setText(sb.toString());


    }

    @Override
    public void saveState() {

    }

    @Override
    public void dispose() {

    }
}
