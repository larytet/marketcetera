package org.marketcetera.photon.commons.ui.databinding;

import org.marketcetera.photon.commons.ReflectiveMessages;
import org.marketcetera.util.log.I18NMessage1P;
import org.marketcetera.util.misc.ClassVersion;

/* $License$ */

/**
 * The internationalization constants used by this package.
 *
 * @author <a href="mailto:will@marketcetera.com">Will Horn</a>
 * @version $Id$
 * @since 2.0.0
 */
@ClassVersion("$Id$")
final class Messages {
    
    static I18NMessage1P REQUIRED_FIELD_SUPPORT_MISSING_VALUE;
    static I18NMessage1P REQUIRED_FIELD_SUPPORT_MISSING_COLLECTION;

    static {
        ReflectiveMessages.init(Messages.class);
    }

    private Messages() {
        throw new AssertionError("non-instantiable"); //$NON-NLS-1$
    }
}
