package code;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class LogEntry {
    private String id;
    private String state;
    private String type;
    private String host;
    private Date timestamp;

    @JsonCreator
    public LogEntry(@JsonProperty(value = "id", required = true) String id,
                    @JsonProperty(value = "state", required = true) String state,
                    @JsonProperty(value = "type") String type,
                    @JsonProperty(value = "host") String host,
                    @JsonProperty(value = "timestamp", required = true) Date timestamp) {
        this.id = id;
        this.state = state;
        this.type = (type == null) ? new String() : type;
        this.host = (host == null) ? new String() : host;
        this.timestamp = timestamp;

    }

    public String getId() {
        return id;
    }

    public String getState() {
        return state;
    }

    public String getType() {
        return type;
    }

    public String getHost() {
        return host;
    }

    public Date getTimestamp() {
        return timestamp;
    }
}