/*
 * Copyright (c) 2003 - 2015 Tyro Payments Limited.
 * 125 York St, Sydney NSW 2000.
 * All rights reserved.
 */
package com.github.jleskovar;

import java.security.SecureRandom;

import org.bitcoin.NativeSecp256k1;
import org.bitcoinj.core.ECKey;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.carrotsearch.junitbenchmarks.BenchmarkRule;

public class Secp256k1KeyGenerationTest {

    @Rule
    public TestRule benchmarkRun = new BenchmarkRule();

    @BenchmarkOptions(benchmarkRounds = 10000, warmupRounds = 0)
    @Test
    public void bitcoinjEckeyPubKeyGeneration() {
        new ECKey().getPubKey();
    }

    @BenchmarkOptions(benchmarkRounds = 10000, warmupRounds = 0)
    @Test
    public void libsecp256k1PubKeyGeneration() throws Exception {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        NativeSecp256k1.computePubkey(randomBytes, 1);
    }

}
