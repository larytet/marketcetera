package org.marketcetera.photon;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.time.DateUtils;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.EAttribute;
import org.marketcetera.core.position.MarketDataSupport;
import org.marketcetera.photon.marketdata.IMarketData;
import org.marketcetera.photon.marketdata.IMarketDataReference;
import org.marketcetera.photon.model.marketdata.MDLatestTick;
import org.marketcetera.photon.model.marketdata.MDMarketstat;
import org.marketcetera.photon.model.marketdata.MDPackage;
import org.marketcetera.util.misc.ClassVersion;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;

/* $License$ */

/**
 * Implements MarketDataSupport for the position engine in Photon. Market data is provided by the
 * common marketdata infrastructure in {@link IMarketData}.
 * 
 * TODO: Cache cleanup when listeners are removed
 * 
 * @author <a href="mailto:will@marketcetera.com">Will Horn</a>
 * @version $Id$
 * @since 1.5.0
 */
@ClassVersion("$Id$")
public class PhotonPositionMarketData implements MarketDataSupport {

	private final IMarketData mMarketData;
	private final ISessionStartTimeProvider mSessionStartTimeProvider;
	private final Adapter mLatestTickAdapter = new LatestTickAdapter();
	private final Adapter mClosingPriceAdapter = new ClosingPriceAdapter();

	/*
	 * mListeners synchronizes access to the following three collections. 
	 */
	private final SetMultimap<String, SymbolChangeListener> mListeners = HashMultimap.create();
	private final Map<String, IMarketDataReference<MDLatestTick>> mLatestTickReferences = Maps
			.newHashMap();
	private final Map<String, IMarketDataReference<MDMarketstat>> mStatReferences = Maps
			.newHashMap();
	/*
	 * These caches allow easy implementation of getLastTradePrice and getClosingPrice.  They also allow notification
	 * to be fired only when the values change to avoid unnecessary notifications, which is especially important with
	 * closing price that rarely changes.
	 */
	private final ConcurrentMap<String, BigDecimal> mLatestTickCache = new ConcurrentHashMap<String, BigDecimal>();
	private final ConcurrentMap<String, BigDecimal> mClosingPriceCache = new ConcurrentHashMap<String, BigDecimal>();
	
	/**
	 * Marks null price for the ConcurrentMap caches which don't allow null
	 */
	private static final BigDecimal NULL = new BigDecimal(Integer.MIN_VALUE);

	/**
	 * Constructor.
	 * 
	 * @param marketData
	 *            the market data provider
	 * @throws IllegalArgumentException
	 *             if marketData is null
	 */
	public PhotonPositionMarketData(IMarketData marketData,
			ISessionStartTimeProvider sessionStartTimeProvider) {
		Validate.noNullElements(new Object[] { marketData, sessionStartTimeProvider });
		mMarketData = marketData;
		mSessionStartTimeProvider = sessionStartTimeProvider;
		mSessionStartTimeProvider.addPropertyChangeListener("sessionStartTime", //$NON-NLS-1$
				new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent evt) {
						// the computed closing price values will likely changed for each symbol
						synchronized (mListeners) {
							for (IMarketDataReference<MDMarketstat> stat : mStatReferences.values()) {
								fireClosingPriceChange(stat.get());
							}
						}
					}
				});
	}

	@Override
	public BigDecimal getLastTradePrice(String symbol) {
		Validate.notNull(symbol);
		// implementation choice to only return the last trade price if it's already known
		// not worth it to set up a new data flow
		return getCachedValue(mLatestTickCache, symbol);
	}

	@Override
	public BigDecimal getClosingPrice(String symbol) {
		Validate.notNull(symbol);
		// implementation choice to only return the closing price if it's already known
		// not worth it to set up a new data flow
		return getCachedValue(mClosingPriceCache, symbol);
	}

	private BigDecimal getCachedValue(final ConcurrentMap<String, BigDecimal> cache, final String symbol) {
		BigDecimal cached = cache.get(symbol);
		return cached == NULL ? null : cached;
	}

	@Override
	public synchronized void addSymbolChangeListener(String symbol, SymbolChangeListener listener) {
		Validate.noNullElements(new Object[] { symbol, listener });
		synchronized (mListeners) {
			IMarketDataReference<MDLatestTick> ref = mLatestTickReferences.get(symbol);
			if (ref == null) {
				ref = mMarketData.getLatestTick(symbol);
				mLatestTickReferences.put(symbol, ref);
				ref.get().eAdapters().add(mLatestTickAdapter);
			}
			IMarketDataReference<MDMarketstat> statRef = mStatReferences.get(symbol);
			if (statRef == null) {
				statRef = mMarketData.getMarketstat(symbol);
				mStatReferences.put(symbol, statRef);
				statRef.get().eAdapters().add(mClosingPriceAdapter);
			}
			mListeners.put(symbol, listener);
		}
	}

	@Override
	public synchronized void removeSymbolChangeListener(String symbol, SymbolChangeListener listener) {
		Validate.noNullElements(new Object[] { symbol, listener });
		synchronized (mListeners) {
			IMarketDataReference<MDLatestTick> ref = mLatestTickReferences.get(symbol);
			IMarketDataReference<MDMarketstat> statRef = mStatReferences.get(symbol);
			Set<SymbolChangeListener> listeners = mListeners.get(symbol);
			listeners.remove(listener);
			if (listeners.isEmpty()) {
				if (ref != null) {
					MDLatestTick tick = ref.get();
					if (tick != null) {
						tick.eAdapters().remove(mLatestTickAdapter);
						mLatestTickReferences.remove(symbol);
						ref.dispose();
					}
				}
				if (statRef != null) {
					MDMarketstat stat = statRef.get();
					if (stat != null) {
						stat.eAdapters().remove(mClosingPriceAdapter);
						mStatReferences.remove(symbol);
						statRef.dispose();
					}
				}
			}
		}
	}

	private void fireSymbolTraded(final MDLatestTick item) {
		fireIfChanged(item.getSymbol(), item.getPrice(), mLatestTickCache, true);
	}

	private void fireClosingPriceChange(final MDMarketstat item) {
		fireIfChanged(item.getSymbol(), computeClosingPrice(item), mClosingPriceCache, false);
	}

	private void fireIfChanged(final String symbol, BigDecimal newPrice,
			final ConcurrentMap<String, BigDecimal> cache,
			final boolean trueForSymbolTradeFalseForClosePrice) {
		// TODO: consider removing items instead of NULL 
		BigDecimal oldPrice = cache.put(symbol, newPrice == null ? NULL : newPrice);
		if (oldPrice == NULL) {
			oldPrice = null;
		}
		// only notify if the value changed
		if (oldPrice == null && newPrice == null) {
			return;
		} else if (oldPrice != null && newPrice != null && oldPrice.compareTo(newPrice) == 0) {
			return;
		}
		SymbolChangeEvent event = new SymbolChangeEvent(PhotonPositionMarketData.this, newPrice);
		// Synchronizing on mListeners, as required per Multimaps#synchronizedMultimap
		// even though the collection is synchronized, iterating over a view needs to be
		// manually synchronized.
		synchronized (mListeners) {
			for (SymbolChangeListener listener : mListeners.get(symbol)) {
				if (trueForSymbolTradeFalseForClosePrice) {
					listener.symbolTraded(event);
				} else {
					listener.closePriceChanged(event);
				}
			}
		}
	}

	private BigDecimal computeClosingPrice(MDMarketstat symbolStatistic) {
		Date sessionStartTime = mSessionStartTimeProvider.getSessionStartTime();
		if (sessionStartTime == null) {
			return null;
		}
		Calendar sessionStartCalendar = Calendar.getInstance();
		sessionStartCalendar.setTime(sessionStartTime);
		Date closeDate = symbolStatistic.getCloseDate();
		if (closeDate != null) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(closeDate);
			if (DateUtils.isSameDay(sessionStartCalendar, calendar)) {
				return symbolStatistic.getClosePrice();
			}
		}
		Date previousCloseDate = symbolStatistic.getPreviousCloseDate();
		if (previousCloseDate != null) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(previousCloseDate);
			if (DateUtils.isSameDay(sessionStartCalendar, calendar)) {
				return symbolStatistic.getPreviousClosePrice();
			}
		}
		return null;
	}
	
	@Override
	public synchronized void dispose() {
		for (Map.Entry<String, SymbolChangeListener> entry : mListeners.entries()) {
			removeSymbolChangeListener(entry.getKey(), entry.getValue());
		}
	}

	private class LatestTickAdapter extends AdapterImpl {

		@Override
		public void notifyChanged(Notification msg) {
			if (!msg.isTouch() && msg.getEventType() == Notification.SET
					&& msg.getFeature() == MDPackage.Literals.MD_LATEST_TICK__PRICE) {
				MDLatestTick item = (MDLatestTick) msg
						.getNotifier();
				fireSymbolTraded(item);
			}
		}
	}

	private class ClosingPriceAdapter extends AdapterImpl {

		private final ImmutableSet<EAttribute> mAttributes = ImmutableSet.of(
				MDPackage.Literals.MD_MARKETSTAT__CLOSE_DATE,
				MDPackage.Literals.MD_MARKETSTAT__CLOSE_PRICE,
				MDPackage.Literals.MD_MARKETSTAT__PREVIOUS_CLOSE_DATE,
				MDPackage.Literals.MD_MARKETSTAT__PREVIOUS_CLOSE_PRICE);

		@Override
		public void notifyChanged(Notification msg) {
			if (!msg.isTouch() && msg.getEventType() == Notification.SET
					&& mAttributes.contains(msg.getFeature())) {
				MDMarketstat item = (MDMarketstat) msg.getNotifier();
				fireClosingPriceChange(item);
			}
		}
	}
}