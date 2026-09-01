package com.gesmio.relay.signing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HmacSignerTest {

    private final HmacSigner signer = new HmacSigner();

    @Test
    void sameInputsProduceSameSignature() {
        String a = signer.sign("{\"hello\":\"world\"}", "s3cr3t");
        String b = signer.sign("{\"hello\":\"world\"}", "s3cr3t");

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSize(64);
    }

    @Test
    void differentSecretsProduceDifferentSignatures() {
        String a = signer.sign("{\"hello\":\"world\"}", "secret-a");
        String b = signer.sign("{\"hello\":\"world\"}", "secret-b");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void differentPayloadsProduceDifferentSignatures() {
        String a = signer.sign("{\"a\":1}", "s3cr3t");
        String b = signer.sign("{\"a\":2}", "s3cr3t");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void generatesUniqueHexSecrets() {
        String a = signer.generateSecret();
        String b = signer.generateSecret();

        assertThat(a).isNotEqualTo(b);
        assertThat(a).hasSize(64);
        assertThat(a).matches("[0-9a-f]+");
    }
}
