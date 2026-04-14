package com.openmailer.openmailer.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalViewAttributesAdvice {

    private final boolean assetMinified;

    public GlobalViewAttributesAdvice(@Value("${app.assets.minified:false}") boolean assetMinified) {
        this.assetMinified = assetMinified;
    }

    @ModelAttribute("assetMinified")
    public boolean assetMinified() {
        return assetMinified;
    }
}
