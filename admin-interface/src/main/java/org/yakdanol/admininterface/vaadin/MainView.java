package org.yakdanol.admininterface.vaadin;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.*;
import com.vaadin.flow.component.combobox.*;
import com.vaadin.flow.component.notification.*;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.radiobutton.*;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import org.yakdanol.admininterface.config.WebClientConfig;
import reactor.core.publisher.Mono;

import java.util.List;

@Route("")
@PageTitle("Traffic Admin")
public class MainView extends VerticalLayout {

    private final WebClient webClient;
    private final WebClientConfig webClientConfig;

    public MainView(WebClient webClient, WebClientConfig webClientConfig) {
        this.webClient = webClient;
        this.webClientConfig = webClientConfig;

        setWidth("400px");
        setJustifyContentMode(JustifyContentMode.CENTER);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        add(createForm());
    }

    private Component createForm() {
        // 1. Какой сервис
        RadioButtonGroup<ServiceKind> serviceKind = new RadioButtonGroup<>();
        serviceKind.setLabel("Задача");
        serviceKind.setItems(ServiceKind.values());
        serviceKind.setValue(ServiceKind.ANALYSIS);

        // 2. Сотрудник
        ComboBox<String> userBox = new ComboBox<>("Сотрудник");
        userBox.setPlaceholder("Загрузка…");
        loadUsers(userBox);                      // <-- новый способ

        // 3. Тип источника
        RadioButtonGroup<DataSource> source = new RadioButtonGroup<>();
        source.setLabel("Тип анализа");
        source.setItems(DataSource.KAFKA, DataSource.FILE);
        source.setValue(DataSource.KAFKA);

        // 4. Действие
        RadioButtonGroup<ActionKind> action = new RadioButtonGroup<>();
        action.setLabel("Действие");
        action.setItems(ActionKind.START, ActionKind.STOP);
        action.setValue(ActionKind.START);

        // Кнопка
        Button send = new Button("Отправить", e -> callMicroservice(
                serviceKind.getValue(),
                action.getValue(),
                source.getValue(),
                userBox.getValue()
        ));
        send.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        send.setEnabled(false);

        userBox.addValueChangeListener(ev ->
                send.setEnabled(userBox.getValue() != null && !userBox.getValue().isBlank())
        );

        return new VerticalLayout(serviceKind, userBox, source, action, send);
    }

    /** Загружаем список ФИО */
    private void loadUsers(ComboBox<String> box) {
        // Формируем URL: http://localhost:8082/api/v1/analysis/users
        String url = webClientConfig.baseUrl(ServiceKind.ANALYSIS)
                + ServiceKind.ANALYSIS.urlPart()
                + "/users";

        // Запросим прямо List<String>
        Mono<List<String>> req = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {});

        UI ui = UI.getCurrent();  // берём текущий UI для безопасных обновлений

        req.subscribe(
                users -> ui.access(() -> {
                    box.setItems(users);
                    box.setPlaceholder("Выберите");
                }),
                error -> ui.access(() ->
                        Notification.show(
                                "Не удалось загрузить список: " + error.getMessage(),
                                4000, Notification.Position.TOP_CENTER))
        );
    }


// 2) Вызов микросервиса с безопасным показом Notification

    private void callMicroservice(ServiceKind svc,
                                  ActionKind act,
                                  DataSource src,
                                  String username) {

        String base = webClientConfig.baseUrl(svc);  // http://localhost:8082 или …:8083
        String path = svc.urlPart() + src.urlPart() + act.urlPart() + "/" + username;

        UI ui = UI.getCurrent();

        webClient.post()
                .uri(base + path)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                        msg -> ui.access(() ->
                                Notification.show(msg, 3000, Notification.Position.TOP_CENTER)),
                        err -> ui.access(() ->
                                Notification.show("Ошибка: " + err.getMessage(),
                                        3000, Notification.Position.TOP_CENTER))
                );
    }

    /* ---------- enum-утилиты (без изменений) ---------------------------- */

    public enum ServiceKind {
        ANALYSIS("/api/v1/analysis"), SECURITY("/api/v1/security");
        private final String base;
        ServiceKind(String base) {
            this.base = base;
        }

        public String urlPart(){
            return base;
        }
        @Override
        public String toString() {
            return this==ANALYSIS ? "Анализ трафика" : "Проверка безопасности";
        }
    }

    public enum DataSource {
        KAFKA("/live/"), FILE("/file/");
        private final String part;

        DataSource(String part) {
            this.part = part;
        }

        public String urlPart() {
            return part;
        }

        @Override
        public String toString() {
            return this==KAFKA ? "Online (Kafka)" : "Offline (файл)";
        }
    }

    public enum ActionKind {
        START("start"), STOP("stop");
        private final String word;

        ActionKind(String w) {
            this.word = w;
        }

        public String urlPart() {
            return word + "/";
        }
        @Override
        public String toString() {
            return this==START ? "Запустить" : "Остановить";
        }
    }
}
