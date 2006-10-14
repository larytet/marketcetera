package org.marketcetera.photon;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.marketcetera.photon.views.AveragePricesViewTest;
import org.marketcetera.photon.views.FIXMessagesViewTest;
import org.marketcetera.photon.views.FillsViewTest;
import org.marketcetera.photon.views.StockOrderTicketViewTest;

public class TS_Photon {
	public static Test suite() {
		TestSuite suite = new TestSuite();
		suite.addTestSuite(StockOrderTicketViewTest.class);
		suite.addTestSuite(FIXMessagesViewTest.class);
		suite.addTestSuite(FillsViewTest.class);
		suite.addTestSuite(AveragePricesViewTest.class);
		return suite;
	}

}
