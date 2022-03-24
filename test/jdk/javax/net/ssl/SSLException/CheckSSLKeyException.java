/*
<<<<<<< HEAD:closed/src/java.base/share/classes/sun/security/provider/NativeSHA.java
 * Copyright (c) 2003, 2014, Oracle and/or its affiliates. All rights reserved.
=======
 * Copyright (C) 2022 THL A29 Limited, a Tencent company. All rights reserved.
>>>>>>> master:test/jdk/javax/net/ssl/SSLException/CheckSSLKeyException.java
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
<<<<<<< HEAD:closed/src/java.base/share/classes/sun/security/provider/NativeSHA.java
/*
 * ===========================================================================
 * (c) Copyright IBM Corp. 2018, 2019 All Rights Reserved
 * ===========================================================================
 */

package sun.security.provider;

public final class NativeSHA extends NativeDigest {

    /**
     * Creates a new native SHA object.
     */
    public NativeSHA() {
        super("SHA-1", 20, 0);
=======

/*
 * @test
 * @bug 8282723
 * @summary Add constructors taking a cause to JSSE exceptions
 */
import javax.net.ssl.SSLKeyException;
import java.util.Objects;

public class CheckSSLKeyException {
    private static String exceptionMessage = "message";
    private static Throwable exceptionCause = new RuntimeException();

    public static void main(String[] args) throws Exception {
        testException(
                new SSLKeyException(exceptionMessage, exceptionCause));
    }

    private static void testException(Exception ex) {
        if (!Objects.equals(ex.getMessage(), exceptionMessage)) {
            throw new RuntimeException("Unexpected exception message");
        }

        if (ex.getCause() != exceptionCause) {
            throw new RuntimeException("Unexpected exception cause");
        }
>>>>>>> master:test/jdk/javax/net/ssl/SSLException/CheckSSLKeyException.java
    }
}
