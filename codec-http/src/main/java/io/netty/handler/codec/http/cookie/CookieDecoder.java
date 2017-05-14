/*
 * Copyright 2015 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.codec.http.cookie;

import static io.netty.handler.codec.http.cookie.CookieUtil.firstInvalidCookieNameOctet;
import static io.netty.handler.codec.http.cookie.CookieUtil.firstInvalidCookieValueOctet;
import static io.netty.handler.codec.http.cookie.CookieUtil.unwrapValue;

import java.nio.CharBuffer;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Parent of Client and Server side cookie decoders
 */
public abstract class CookieDecoder {

    private final InternalLogger logger = InternalLoggerFactory.getInstance(getClass());

    private final boolean strict;
    private final boolean reportException;

    protected CookieDecoder(boolean strict, boolean reportException) {
       this.strict = strict;
       this.reportException = reportException;
    }

    protected CookieDecoder(boolean strict) {
        this(strict, false);
    }

    protected DefaultCookie initCookie(String header, int nameBegin, int nameEnd, int valueBegin, int valueEnd) {
        if (nameBegin == -1 || nameBegin == nameEnd) {
            if (logger.isDebugEnabled()) {
                logger.debug("Skipping cookie with null name");
            }
            if (reportException) {
                throw new IllegalArgumentException("Skipping cookie with null name");
            }
            return null;
        }

        if (valueBegin == -1) {
            if (logger.isDebugEnabled()) {
                logger.debug("Skipping cookie with null value");
            }
            if (reportException) {
                throw new IllegalArgumentException("Skipping cookie with null value");
            }
            return null;
        }

        CharSequence wrappedValue = CharBuffer.wrap(header, valueBegin, valueEnd);
        CharSequence unwrappedValue = unwrapValue(wrappedValue);
        if (unwrappedValue == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Skipping cookie because starting quotes are not properly balanced in '{}'", wrappedValue);
            }
            if (reportException) {
                throw new IllegalArgumentException(
                        String.format("Skipping cookie because starting quotes are not properly balanced in '%s'",
                                wrappedValue));
            }
            return null;
        }

        final String name = header.substring(nameBegin, nameEnd);

        int invalidOctetPos;
        if (strict && (invalidOctetPos = firstInvalidCookieNameOctet(name)) >= 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("Skipping cookie because name '{}' contains invalid char '{}'",
                        name, invalidOctetPos);
            }
            if (reportException) {
                throw new IllegalArgumentException(
                        String.format("Skipping cookie because name '%s' contains invalid char '%c'",
                                name, invalidOctetPos));
            }
            return null;
        }

        final boolean wrap = unwrappedValue.length() != valueEnd - valueBegin;

        if (strict && (invalidOctetPos = firstInvalidCookieValueOctet(unwrappedValue)) >= 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("Skipping cookie because value '{}' contains invalid char '{}'",
                    unwrappedValue, invalidOctetPos);
            }
            if (reportException) {
                throw new IllegalArgumentException(
                        String.format("Skipping cookie because value '%s' contains invalid char '%c'",
                                unwrappedValue, invalidOctetPos));
            }
            return null;
        }

        DefaultCookie cookie = new DefaultCookie(name, unwrappedValue.toString());
        cookie.setWrap(wrap);
        return cookie;
    }
}
