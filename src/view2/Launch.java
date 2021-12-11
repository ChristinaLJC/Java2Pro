package view2;

import data.Data;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import tool.*;
import util.Holder;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

@SuppressWarnings({"RedundantIfStatement", "unused"})
public class Launch extends Application {

    /**
     * 该方法用于根据参数表创建一个窗口的主页。<br>
     * <p/>
     *
     * 你可以通过一个映射表来描述你希望设置的参数信息。<br>
     * <p/>
     *
     * 例如，"prefWidth" 将会设置该主页的默认宽度，"prefHeight" 设置默认高度。<br>
     * "style" 设置该主页的 CSS 风格。<br>
     * <p/>
     *
     * @param argumentsMap 设置主页的参数表。
     * @return 主页（BorderPane）
     */
    private static BorderPane initMainPane(HashMap<String, Object> argumentsMap) {
        BorderPane root = new BorderPane();
        root.setPrefWidth((Double) argumentsMap.getOrDefault("prefWidth", 1e3));
        root.setPrefHeight((Double) argumentsMap.getOrDefault("prefHeight", 8e2));
        root.setStyle((String) argumentsMap.getOrDefault("style", "-fx-background-color: #FF9900;"));

        initTipBox(root, argumentsMap);

        return root;
    }

    /**
     * 初始化提示框，并将提示框的控制命令放入
     * @param root 提示框所在页，也是我们 GUI 的显示主体
     * @param argumentsMap 初始化提示框的相关参数选择！
     */
    private static void initTipBox(BorderPane root, HashMap<String, Object> argumentsMap) {
        // 创建消息提示框并设置它的位置。
        VBox tipBox = new VBox();
        root.setBottom(tipBox);

        // 设置消息提示框的样式。
        tipBox.setPadding(new Insets((Double) argumentsMap.getOrDefault("tipBoxInsets", 5.0)));
        tipBox.setBorder(new Border(new BorderStroke(Color.LIGHTGREY, BorderStrokeStyle.SOLID,
                new CornerRadii(5), new BorderWidths(2))));
        tipBox.setStyle((String) argumentsMap.getOrDefault("tipBoxStyle", "-fx-background-color:#CCFFFF"));

        // 提示框内容设置
        final Label message = new Label("");
        tipBox.getChildren().add(message);

        // 将对提示框的内容设置方法上传到全局信息，便于其他方法进行调用。
        // 补充：新增了多线程的并发支持。
        tipNotify = new Consumer<String>() {
            @Override synchronized
            public void accept(String s) {
                message.setText(s);
            }
        };

        // 设置提示框消失动画。
        FadeTransition goDeath = new FadeTransition(Duration.seconds(2), tipBox);
        goDeath.setAutoReverse(false);
        goDeath.setToValue(0.);
        FadeTransition goLive = new FadeTransition(Duration.seconds(2), tipBox);
        goLive.setAutoReverse(false);
        goLive.setToValue(1.);
        Holder<Boolean> isDeath = new Holder<>(); isDeath.obj = false;

        MenuItem displayTip = new MenuItem("显示/隐藏提示框");
        displayTip.setOnAction(event -> {
            isDeath.obj = !isDeath.obj;
            BorderPane root1 = (BorderPane) storeMap.get("root");
            if (!isDeath.obj) {
                root1.setBottom(tipBox);
                goLive.play();
            } else {
                goDeath.play();
                final Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(2.), e -> root1.bottomProperty().set(null)));
                timeline.setCycleCount(1);
                timeline.play();
            }
        });
        displayTip.setAccelerator(new KeyCodeCombination(KeyCode.T, KeyCombination.ALT_DOWN));
        storeMap.put("displayOption", displayTip);
    }

    /**
     *
     */
    private static final Map<String, Object> storeMap = new HashMap<>();

    private static final ToIntFunction<String> cntSupplier = new ToIntFunction<String>() {
        private final Map<String, Integer> supplierMap = new HashMap<>();
        @Override
        synchronized
        public int applyAsInt(String s) {
            if (!supplierMap.containsKey(s)) {
                supplierMap.put(s, 1);
            } else supplierMap.put(s, supplierMap.get(s) + 1);
            return supplierMap.get(s);
        }
    };

    /**
     * 通过调用该方法实现对提示框内信息的控制。<br>
     * <p/>
     *
     * 该方法提供了并发支持。<br>
     * 请不要担心在多线程并发情况下该方法的错误。<br>
     * <p/>
     *
     *
     */
    private static Consumer<String> tipNotify;

    /**
     * 该对象封装了对标签页的方法提供。<br>
     * <p/>
     *
     * 通过映射表可以设置特定的参数以便于获取该标签页的性质进行修改。<br>
     * <p/>
     *
     * 建议使用 {@link TabArgumentMap} 对标签页的性质进行初始化。<br>
     * 你可以很容易地确定你可以设置的属性名和其相关的类型参数。<br>
     * <p/>
     */
    private static final Function<Map<String, Object>, Tab> tabSupplier = (map) -> {
        Tab returnTab = new Tab();

        if (!map.containsKey("title")) {
            String type = map.getOrDefault("type", "New Page").toString();
            map.put("title", type + " " + cntSupplier.applyAsInt(type));
        }
        returnTab.setText(map.get("title").toString());

        // 设置该标签页内部的页面框架。
        BorderPane viewPane = new BorderPane();
        returnTab.setContent(viewPane);

        // 设置该标签页右边的相关选项框、搜索框。
        HBox searchPane = new HBox();
        searchPane.setPadding(new Insets(20));
        viewPane.setRight(searchPane);

        // 设置搜索、选择页的相关风格
        searchPane.setStyle((String )map.getOrDefault("searchPaneStyle", "-fx-background-color: #CCFF99;"));
        // 设置搜索、选择页的宽度
        searchPane.setPrefWidth((double)map.getOrDefault("searchPanePrefWidth", 200.));

        //设置table页面
        if (map.getOrDefault("type", DisplayType.TABLE) == DisplayType.TABLE) {

            final Holder<Consumer<String>> searchBoxActionHolder = new Holder<>();

            // 创建搜索框等相关操作
            {
                // 搜索框
                VBox searchBox = new VBox();
                searchBox.setFocusTraversable(false);
                searchBox.setSpacing(10);
                searchBox.setAlignment(Pos.TOP_RIGHT);

                // 可键入的搜索框初始化
                TextField searchField = new TextField();
                // 可以通过快捷键选中搜索文本框
                searchField.setFocusTraversable(true);

                // 新增搜索框的大小初始化描述
                searchField.setPrefSize((double )map.getOrDefault("searchPrefWidth", 150.),
                        (double )map.getOrDefault("searchPrefHeight", 20.));

                // 搜索会发生的事情
                final Consumer<String> searchAction = (searchContent) -> searchBoxActionHolder.obj.accept(searchContent);

                // 增添提示信息
                searchField.setPromptText(map.getOrDefault("searchPromptText", "请输入关键词").toString());
                searchField.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> tipNotify.accept(map.getOrDefault("searchTip", "输入关键词以搜索相关信息").toString()));
                searchField.addEventHandler(MouseEvent.MOUSE_EXITED, e -> tipNotify.accept(""));


                // 创建搜索按钮
                Button searchConfirmButton = new Button(map.getOrDefault("searchButton", "搜索").toString());
                searchConfirmButton.setPrefSize((double )map.getOrDefault("searchButtonPrefWidth", 50.),
                        (double )map.getOrDefault("searchPrefHeight", 20.));

                // 新增搜索按钮的提示信息
                searchConfirmButton.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> tipNotify.accept(map.getOrDefault("searchButtonTip", "点击确认搜索").toString()));
                searchConfirmButton.addEventHandler(MouseEvent.MOUSE_EXITED, e -> tipNotify.accept(""));

                // 将搜索框和搜索按钮一同添加到搜索组件中。
                searchBox.getChildren().addAll(searchField, searchConfirmButton);

                // 搜索组件放入右侧快捷栏中。
                searchPane.getChildren().add(searchBox);

                // 集中处理搜索事件
                searchField.setOnAction(e -> searchAction.accept(searchField.getText()));
                searchConfirmButton.setOnAction(e -> searchAction.accept(searchField.getText()));

                //todo: 还想不到好的GUI设计，暂时这样呈现，有点点丑
                DatePicker datePicker = new DatePicker(LocalDate.now());
                datePicker.setEditable(false);
                searchBox.getChildren().add(datePicker);

                datePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
                    String date = newValue.format(DateTimeFormatter.ISO_DATE);
                    searchField.setText(date);
                });


            }


            // 创建图表的相关操作
            {
                @SuppressWarnings("unchecked")
                TableView<Tmp> tableRowTableView = initTableView((List<String>)map.get("colNames"), (List<Data>) map.get("rows"));
                viewPane.setCenter(tableRowTableView);
                // 稍稍设置一下相关的图形参数吧，让它好看点
                tableRowTableView.setPadding(new Insets(20));

                // 设置搜索会发生的事情
                @SuppressWarnings("unchecked") final List<Data> rows = (List<Data>) map.get("rows");

                searchBoxActionHolder.obj = (searchText) -> {
                    ObservableList<Tmp> searchList = FXCollections.observableArrayList();

                    rows.stream().filter(d -> {
                        if (d.fetch("location").contains(searchText))
                            return true;
                        if (d.fetch("iso code").contains(searchText))
                            return true;
                        if (d.fetch("date").equals(searchText))
                            return true;
                        return false;
                    }).map(Tool::createRow).forEach(searchList::add);

                    tableRowTableView.setItems(searchList);
                };

            }

        }
        //设置graph页面
        else if (map.get("type") == DisplayType.GRAPH){

            // 大工程hhh, 怎么做图呢？

            // 先规约一个图的细选择吧。。。
            {
                BorderPane chartPane = new BorderPane();
                viewPane.setCenter(chartPane);
                // First step: 获取将要创建的图表类型
                switch ((GraphType) map.get("graphType")) {
                    case PIE_CHART:
                        break;
                    case BUBBLE_CHART:
                        break;
                    case LINE_CHART:

                        ObservableList<String> dateList = FXCollections.observableArrayList();
                        ObservableList<XYChart.Series<String, Number>> countriesList = FXCollections.observableArrayList();
                        Map<String, XYChart.Series<String, Number>> splitMap = new HashMap<>();

                        Holder<Consumer<Void>> initPerform = new Holder<>();
                        initPerform.obj = (V) -> {};

                        {
                            Axis<String> date = new CategoryAxis(dateList);

                            Axis<Number> values = new NumberAxis();

                            LineChart<String, Number> chart = new LineChart<>(date, values, countriesList);
                            chart.setCreateSymbols(false);

                            chartPane.setCenter(chart);

                            String mainColName = (String ) map.get("major");
                            String minorColName = (String ) map.get("minor");
                            String valueColumn = (String ) map.get("value");

                            @SuppressWarnings("unchecked")
                            TreeMap<String, List<Data>> dateToData = new TreeMap<>((Comparator<? super String>) map.getOrDefault("dateComparator", null));

                            @SuppressWarnings("unchecked")
                            List<Data> rows = (List<Data>) map.get("rows");

                            rows.forEach(r -> {
                                if (!dateToData.containsKey(r.fetch(minorColName))) {
                                    dateToData.put(r.fetch(minorColName), new ArrayList<>());
                                }
                                dateToData.get(r.fetch(minorColName)).add(r);
                            });

                            Iterator<Map.Entry<String, List<Data>>> iterator = dateToData.entrySet().iterator();

                            Timeline animationOnce = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                                if (iterator.hasNext()) {
                                    Map.Entry<String, List<Data>> next = iterator.next();
                                    dateList.add(next.getKey());
                                    next.getValue().forEach(r -> {
                                        if (!splitMap.containsKey(r.fetch(mainColName))) {
                                            XYChart.Series<String, Number> countrySeries = new XYChart.Series<>();
                                            countrySeries.setName(r.fetch(mainColName));
                                            splitMap.put(r.fetch(mainColName), countrySeries);
                                            countriesList.add(countrySeries);
                                        }
                                        splitMap.get(r.fetch(mainColName)).getData().add(new XYChart.Data<>(r.fetch(minorColName),
                                                (Double.parseDouble("0" + r.fetch(valueColumn)))
                                        ));
                                    });
                                }
                            }));

                            animationOnce.setAutoReverse(false);
                            animationOnce.setCycleCount(dateToData.size());

                            // 为原来的初始化操作新增一个动作！现在初始化的过程将会播放动画！
                            initPerform.obj = initPerform.obj.andThen((V) -> animationOnce.play());
                        }


                        initPerform.obj.accept(null);

                        break;
                }
            }

        }

        return returnTab;
    };

    private static TableView<Tmp> initTableView(List<String> colNames, List<Data> datas) {
        TableView<Tmp> view = new TableView<>();
        view.setTableMenuButtonVisible(true);

        final ToIntFunction<String> widthSupplier = str -> 80;

        final Function<String, TableColumn<Tmp, String>> colGenerator =
                s -> {
                    final TableColumn<Tmp, String> col = new TableColumn<>(s);
                    col.setCellValueFactory(new PropertyValueFactory<>(s));
                    col.setPrefWidth(widthSupplier.applyAsInt(s));
                    return col;
                };
        colNames.forEach(cn -> view.getColumns().add(colGenerator.apply(Tool.transferReverse(cn))));

//        datas.stream().map(Tool::createRow).forEach(view.getItems()::add);
        ObservableList<Tmp> dataList = FXCollections.observableArrayList();
        datas.stream().map(Tool::createRow).forEach(dataList::add);
        view.setItems(dataList);
        return view;
    }

    private static TabPane initTabPane(Tab... tabs) {
        TabPane tabPane = new TabPane(tabs);
        if (tabs.length == 0) {
            FileController fileData = Controller.instance.getFileData(Paths.get("res", "file", "owid-covid-data.csv").toFile());
            tabPane.getTabs().addAll(tabSupplier.apply(new TabArgumentMap().type(DisplayType.TABLE)
                    .colNames(fileData.basicListColName).rows(fileData.basicList)));
        }
        return tabPane;
    }

    private static boolean createTmpRowClassFlag = false;

    // 经过艰难的努力，该方法终于被弃用啦～

    @Deprecated
    private static TableView<Tmp> initTableView() {
        TableView<Tmp> table = new TableView<>();
        table.setPrefSize(560, 500);
        table.setTableMenuButtonVisible(true);

        // 根据列名设置列的宽度
        final ToIntFunction<String> widthSupplier = str -> 80;

        // 根据列名设置该列的相关信息
        final Function<String, TableColumn<Tmp, String>> normalColGenerator =
                s -> {
                    final TableColumn<Tmp, String> col = new TableColumn<>(s);
                    col.setCellValueFactory(new PropertyValueFactory<>(s));
                    col.setPrefWidth(widthSupplier.applyAsInt(s));
                    return col;
                };

        // Java 风格的变量名变化。
        final Function<String, String> strMap = Tool::transferReverse;

        // 获取列名及各行信息
        FileController fileData = Controller.instance.getFileData(Paths.get("res", "file", "owid-covid-data.csv").toFile());
        if (!createTmpRowClassFlag) {
            final Predicate<String> intPropertyColumnPredicate = s -> {
                if (s.endsWith("Cases"))
                    return true;
                if (s.endsWith("Deaths"))
                    return true;
                if (s.equals("population"))
                    return true;
                if (s.endsWith("Tests")) {
                    return true;
                }
                if (s.endsWith("Admissions"))
                    return true;
                if (s.endsWith("Patients"))
                    return true;
                if (s.endsWith("Vaccinations"))
                    return true;
                return false;
            };
            final Predicate<String> doublePropertyColumnPredicate = s -> {
                if (s.endsWith("Rate"))
                    return true;
                if (s.contains("Per"))
                    return true;
                if (s.endsWith("Smoothed"))
                    return true;
                if (s.endsWith("Density"))
                    return true;
                return false;
            };
            List<String> ints = new ArrayList<>();
            List<String> doubles = new ArrayList<>();
            List<String> strings = new ArrayList<>();
            fileData.basicListColName.stream().map(Tool::transferReverse).forEach(f -> {
                if (intPropertyColumnPredicate.test(f))
                    ints.add(f);
                else if (doublePropertyColumnPredicate.test(f))
                    doubles.add(f);
                else
                    strings.add(f);
            });
            Tool.createClass(strings, ints, doubles);
            createTmpRowClassFlag = true;
        }
        List<Data> allData = fileData.higherList;
        Holder<List<String>> holder = new Holder<>();
        holder.obj = fileData.higherListColName;

        // 创建各列信息
        holder.obj.forEach(s -> table.getColumns().add(strMap.andThen(normalColGenerator).apply(s)));

        ObservableList<Tmp> tableData = FXCollections.observableArrayList();
        table.setItems(tableData);

        allData.stream().map(Tool::createRow).forEach(tableData::add);

        return table;
    }

    /**
     * 该方法会初始化 JavaFX 的菜单栏。<br>
     * <p/>
     *
     * 该方法将会初始化出 Application layer 的菜单栏。<br>
     * 建议使用弱链接【高耦合】映射表延迟绑定菜单栏的功能实现。<br>
     * 因为菜单栏在很早的时期便进行初始化。<br>
     * 而其他的功能界面此时还尚未开始创建！<br>
     * <p/>
     *
     * @return 初始化后的菜单栏。
     */
    private static MenuBar initMenuBar(Scene accelerateScene) {
        MenuBar menuBar = new MenuBar();

        // 设置该菜单栏为该应用程序的系统级菜单栏
        menuBar.setUseSystemMenuBar(true);

        Menu menuFile = new Menu("文件");
        Menu menuData = new Menu("数据");
        Menu help = new Menu("帮助");
        menuBar.getMenus().addAll(menuFile, menuData, help);

        {
            // 设置显示、隐藏提示框的操作，并增加新的快捷键
            MenuItem displayOption = (MenuItem) storeMap.get("displayOption");
            help.getItems().add(displayOption);

            // 快捷键为 Alt + T, means optional to see tip box.
            KeyCombination combination = new KeyCodeCombination(KeyCode.T, KeyCombination.ALT_DOWN);
            accelerateScene.getAccelerators().put(combination, displayOption::fire);
        }

        MenuItem tableMenu = new MenuItem("表");
        menuData.getItems().add(tableMenu);
        tableMenu.setOnAction(event -> {
            DialogPane setNamePane = new DialogPane();
            setNamePane.setHeaderText("Set Table Name");
            TextField name = new TextField();
            setNamePane.setContent(name);
            setNamePane.getButtonTypes().add(ButtonType.CANCEL);
            setNamePane.getButtonTypes().add(ButtonType.APPLY);

            Scene stageScene = new Scene(setNamePane);
            Stage stage = new Stage();
            stage.setWidth(300);
            stage.setTitle("Set Name");
            stage.setScene(stageScene);
            stage.show();

            final Consumer<Void> tableAction = (o) -> {
                Map<String, Object> map;
                if (!name.getText().equals("")) {
                    map = new TabArgumentMap().title(name.getText()).type(DisplayType.TABLE);
                }
                else map = new TabArgumentMap().type(DisplayType.TABLE);

                Tab apply = tabSupplier.apply(map);

                TabPane tabPane1 = (TabPane) storeMap.get("tabPane");

                tabPane1.getTabs().add(apply);
                tabPane1.getSelectionModel().select(apply);
                stage.close();
            };

            //给apply按钮设置action
            Button applyButton = (Button) setNamePane.lookupButton(ButtonType.APPLY);
            // 设置允许聚焦选项
            applyButton.setFocusTraversable(true);
            // 将默认的窗口聚焦拉到它上面！
            applyButton.requestFocus();
            applyButton.setOnAction(e -> tableAction.accept(null));
            Tool.setEnterKeyForButton(applyButton);

            // 设置取消按钮
            Button cancelButton = (Button) setNamePane.lookupButton(ButtonType.CANCEL);
            cancelButton.setFocusTraversable(true);
            cancelButton.setOnAction(e -> stage.close());
            Tool.setEnterKeyForButton(cancelButton);
        });

        MenuItem graphMenu = new MenuItem("Graph");
        menuData.getItems().add(graphMenu);

        graphMenu.setOnAction(event -> {
            DialogPane setNamePane = new DialogPane();
            setNamePane.setHeaderText("设置图页名称");
            TextField name = new TextField();
            setNamePane.setContent(name);
            setNamePane.getButtonTypes().add(ButtonType.CANCEL);
            setNamePane.getButtonTypes().add(ButtonType.APPLY);

            Scene stageScene = new Scene(setNamePane);
            Stage stage = new Stage();
            stage.setWidth(300);
            stage.setTitle("设置图页名称");
            stage.setScene(stageScene);
            stage.show();

            final Consumer<Object> graphAction = (o) -> {

                final FileController lists = Controller.instance.getFileData(Paths.get("res", "file", "owid-covid-data.csv").toFile());

                Map<String, Object> map;
                if (!name.getText().equals("")) {
                    map = new TabArgumentMap().type(DisplayType.GRAPH).title(name.getText());
                }
                else map = new TabArgumentMap().type(DisplayType.GRAPH).colNames(lists.basicListColName).rows(lists.basicList)
                        .major("location").minor("date").value("total cases").graphType(GraphType.LINE_CHART);

                Tab apply = tabSupplier.apply(map);
                TabPane tabPane = (TabPane) storeMap.get("tabPane");
                tabPane.getTabs().add(apply);
                tabPane.getSelectionModel().select(apply);
                stage.close();
            };

            // [Warning]: 请不要使用这样的方式设置快捷键
//            //设置Enter快捷键
//            stageScene.setOnKeyPressed(e -> {
//                if (e.getCode() == KeyCode.ENTER)
//                    graphAction.accept(new Object());
//            });

            //给apply按钮设置action
            Button applyButton = (Button) setNamePane.lookupButton(ButtonType.APPLY);
            applyButton.setOnAction(e -> graphAction.accept(new Object()));

            Button cancelButton = (Button) setNamePane.lookupButton(ButtonType.CANCEL);
            cancelButton.setOnAction(e -> stage.close());
        });

        return menuBar;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        BorderPane root = initMainPane(new HashMap<>());
        storeMap.put("root", root);

        Scene scene = new Scene(root);
        storeMap.put("scene", scene);

        // 设置菜单栏位于主页上面。
        // 注意到，菜单栏设置时，其主体页面还尚未被创建。
        root.setTop(initMenuBar(scene));

        TabPane tabPane1 = initTabPane();
        storeMap.put("tabPane", tabPane1);
        root.setCenter(tabPane1);

        //全屏/窗口模式切换
        primaryStage.addEventHandler(KeyEvent.KEY_RELEASED, e -> {
            if (e.getCode() == KeyCode.F11)
                primaryStage.setFullScreen(!primaryStage.isFullScreen());
        });

        primaryStage.setFullScreenExitHint("按 F11 切换全屏/窗口模式");
        primaryStage.setFullScreen(false);

        primaryStage.setScene(scene);
        primaryStage.setHeight(630);
        primaryStage.setWidth(1050);
        primaryStage.setTitle("COVID-19 TRACING");
        primaryStage.getIcons().add(new Image("file:"+System.getProperty("user.dir")+"/res/picture/icon1.png"));

        primaryStage.show();
    }
}
