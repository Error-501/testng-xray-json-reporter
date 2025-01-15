package xray.json.model.misc;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomField {
        private String id;
        private String name;
        private Object value;

        public CustomField(String id, Object value) {
                this.id = name;
                this.value = value;
        }
}