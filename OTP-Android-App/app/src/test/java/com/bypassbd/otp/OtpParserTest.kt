package com.bypassbd.otp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.bypassbd.otp.data.OtpParser

class OtpParserTest {

    @Test fun parsesTheIvacWordSequence() {
        val sms = "(IVACBD) For security, type the following sequence when prompted " +
            "Four-Two-Six-One-Four-Seven ."
        assertEquals("426147", OtpParser.extract(sms))
    }

    @Test fun parsesSpaceSeparatedWords() {
        assertEquals("1234", OtpParser.extract("Your code is One Two Three Four"))
    }

    @Test fun fallsBackToPlainDigits() {
        assertEquals("426147", OtpParser.extract("IVAC OTP is 426147"))
    }

    @Test fun handlesZeroWords() {
        assertEquals("4026", OtpParser.extract("Four-Zero-Two-Six"))
    }

    @Test fun ignoresNonCodeWords() {
        // "for", "the", "sequence" are not number words and must not corrupt the run.
        assertEquals("426147", OtpParser.extract("type the sequence Four-Two-Six-One-Four-Seven now"))
    }

    @Test fun returnsNullWhenNoCode() {
        assertNull(OtpParser.extract("Welcome to IVAC. Please visit our website."))
        assertNull(OtpParser.extract(""))
        assertNull(OtpParser.extract(null))
    }

    @Test fun detectsIvacMessages() {
        assertTrue(OtpParser.looksLikeIvac("IVACBD",
            "type the following sequence Four-Two-Six-One-Four-Seven"))
        assertTrue(OtpParser.looksLikeIvac(null,
            "(IVACBD) For security, type the following sequence when prompted Four-Two-Six-One-Four-Seven"))
    }

    @Test fun picksLongestRunNotTheFirst() {
        // A stray "one" earlier must lose to the real 6-digit block.
        assertEquals("426147", OtpParser.extract("in one message: Four-Two-Six-One-Four-Seven"))
    }
}
