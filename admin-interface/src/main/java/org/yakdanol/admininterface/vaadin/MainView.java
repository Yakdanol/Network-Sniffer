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
import java.util.Set;

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
        MultiSelectComboBox<String> userBox = new MultiSelectComboBox<>("Сотрудник");
        userBox.setPlaceholder("Загрузка…");
        userBox.setWidth("320px");
        loadUsers(userBox);

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

        // Кнопка отправки
        Button send = new Button("Отправить", e -> callMicroservice(
                serviceKind.getValue(),
                action.getValue(),
                source.getValue(),
                userBox.getValue()
        ));
        send.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        send.setEnabled(false);

        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        userBox.addValueChangeListener(ev ->
                send.setEnabled(!userBox.getSelectedItems().isEmpty()));

        return new VerticalLayout(serviceKind, userBox, source, action, send);
    }

    /** Загружаем список ФИО */
    private void loadUsers(MultiSelectComboBox<String> box) {
        // 1) Формируем URL: http://*/api/v1/analysis/users
        String url = webClientConfig.baseUrl(ServiceKind.ANALYSIS)
                + ServiceKind.ANALYSIS.urlPart()
                + "/users";

        // 2) Запросим List<String> ФИО
        Mono<List<String>> req = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {});

        // 3) Берём текущий UI для безопасных обновлений
        UI ui = UI.getCurrent();

        req.subscribe(
                users -> ui.access(() -> {
                    box.setItems(users);               // наполняем MultiSelectComboBox
                    box.setPlaceholder("Выберите");
                }),
                err -> ui.access(() ->      // показываем Notification в случае ошибки
                        Notification.show(
                                "Не удалось загрузить список сотрудников: "
                                        + err.getMessage(),
                                4000, Notification.Position.TOP_CENTER))
        );
    }


// 2) Вызов микросервиса с показом Notification
private void callMicroservice(ServiceKind svc,
                              ActionKind act,
                              DataSource src,
                              Set<String> selectedUsers) {
    // Вычисляем базовый URL: analysis или security
    String base = webClientConfig.baseUrl(svc);

    // Определяем, batch-запрос или одиночный
    boolean batch = selectedUsers.size() > 1;

    // Строим путь
    String path = svc.urlPart() + src.urlPart() + act.urlPart();
    WebClient.RequestBodySpec spec = webClient.post().uri(base + path);

    Mono<String> response;
    if (batch) {
        response = spec
                .bodyValue(selectedUsers)
                .retrieve()
                .bodyToMono(String.class);
    } else {
        String user = selectedUsers.iterator().next();
        response = webClient.post()
                .uri(base + path + "/" + user)
                .retrieve()
                .bodyToMono(String.class);
    }

    // Получаем UI для Notification
    UI ui = UI.getCurrent();

    response.subscribe(
            msg -> ui.access(() ->
                    Notification.show(msg, 4000, Notification.Position.TOP_CENTER)
            ),
            err -> ui.access(() ->
                    Notification.show("Ошибка: " + err.getMessage(),
                            4000, Notification.Position.TOP_CENTER)
            )
    );
}

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
