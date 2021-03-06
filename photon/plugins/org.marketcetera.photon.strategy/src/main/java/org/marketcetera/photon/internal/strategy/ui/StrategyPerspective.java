package org.marketcetera.photon.internal.strategy.ui;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.marketcetera.photon.strategy.StrategyUI;
import org.marketcetera.photon.strategy.engine.ui.workbench.StrategyEngineWorkbenchUI;
import org.marketcetera.util.misc.ClassVersion;

/* $License$ */

/**
 * Initialized the Strategy perspective.
 * 
 * @author <a href="mailto:will@marketcetera.com">Will Horn</a>
 * @version $Id$
 * @since 1.0.0
 */
@ClassVersion("$Id$")
public class StrategyPerspective implements IPerspectiveFactory {

    @Override
    public void createInitialLayout(IPageLayout layout) {
        String editorArea = layout.getEditorArea();
        layout.addView(StrategyUI.PROJECT_EXPLORER_VIEW,
                IPageLayout.LEFT, 0.26f, editorArea);
        layout.addView(StrategyEngineWorkbenchUI.STRATEGY_ENGINES_VIEW_ID,
                IPageLayout.BOTTOM, 0.65f,
                StrategyUI.PROJECT_EXPLORER_VIEW);

        IFolderLayout bottom = layout.createFolder(
                "bottom", IPageLayout.BOTTOM, 0.65f, editorArea);//$NON-NLS-1$
        bottom.addView(StrategyUI.TRADE_SUGGESTIONS_VIEW);

        layout.addView(IPageLayout.ID_OUTLINE, IPageLayout.RIGHT, 0.75f,
                editorArea);
    }
}
