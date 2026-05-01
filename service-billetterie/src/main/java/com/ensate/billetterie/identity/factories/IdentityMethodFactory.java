package com.ensate.billetterie.identity.factories;

import com.ensate.billetterie.identity.domain.IdentityMethodType;
import com.ensate.billetterie.identity.implementations.FaceIDImpl;
import com.ensate.billetterie.identity.implementations.FingerprintImpl;
import com.ensate.billetterie.identity.implementations.QRCodeImpl;
import com.ensate.billetterie.identity.interfaces.IdentityMethod;

import java.util.Map;

public class IdentityMethodFactory {

    private static final Map<IdentityMethodType, IdentityMethod> REGISTRY = Map.of(
            IdentityMethodType.QR_CODE,     new QRCodeImpl(),
            IdentityMethodType.FINGERPRINT, new FingerprintImpl(),
            IdentityMethodType.FACE_ID,     new FaceIDImpl()
    );

    public static IdentityMethod create(IdentityMethodType type) {
        IdentityMethod method = REGISTRY.get(type);
        if (method == null) {
            throw new IllegalArgumentException("No identity method registered for type: " + type);
        }
        return method;
    }

    private IdentityMethodFactory() {}
}
