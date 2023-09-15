package com.alash.medict.event;

import com.alash.medict.model.User;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Getter
@Setter
public class RegistrationCompletePublisher extends ApplicationEvent {

    private User user;
    private String applicationUrl;

    public RegistrationCompletePublisher(User user, String applicationUrl) {
        super(user);
        this.user = user;
        this.applicationUrl = applicationUrl;
    }
}
