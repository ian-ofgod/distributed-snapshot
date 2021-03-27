package library;

import java.io.Serializable;

public class Message implements Serializable {
    String message;
    Message(String msg){
        message=msg;
    }
}
