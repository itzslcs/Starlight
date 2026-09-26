package dev.mw19.api.name;

import dev.mw19.api.Subscription;

public interface NameTags {
    Subscription register(NameDecorator decorator);
}
