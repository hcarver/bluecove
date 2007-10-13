/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007 Vlad Skarzhevskyy
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *  @version $Id$
 */
package javax.obex;

import net.sf.jour.signature.SignatureTestCase;

/**
 * @author vlads
 *
 */
public class JSR82APIObexDeclarationsTest extends SignatureTestCase {

    /* (non-Javadoc)
     * @see net.sf.jour.signature.SignatureTestCase#getAPIPath()
     */
    public String getAPIPath() {
        return getClassPath(Authenticator.class);
    }

    /* (non-Javadoc)
     * @see net.sf.jour.signature.SignatureTestCase#getSignatureXMLPath()
     */
    public String getSignatureXMLPath() {
        return "jsr82-obex-signature.xml";
    }

}
