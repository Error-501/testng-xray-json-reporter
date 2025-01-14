package xray.json.model.test;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestStep {
    private String action;
    private String data;
    private String result;
}