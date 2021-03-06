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
package de.longri.cachebox3.gui.actions;

import de.longri.cachebox3.CB;
import de.longri.cachebox3.gui.menu.MenuID;
import de.longri.cachebox3.gui.views.AboutView;
import de.longri.cachebox3.gui.views.TestView;

/**
 * Created by Longri on 24.07.16.
 */
public class Action_Show_TestView extends Action {

    public Action_Show_TestView() {
        super("TestView", MenuID.AID_TEST_VIEW);
    }


    @Override
    protected void Execute() {
        if (CB.viewmanager.getActView() instanceof TestView) return;

        TestView view = new TestView();
        CB.viewmanager.showView(view);

    }
}
