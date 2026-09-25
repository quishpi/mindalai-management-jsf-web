package ec.mindalai.managementjsf.controller;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.Getter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@SessionScoped
public class GuestPreferences implements Serializable {

    private static final long serialVersionUID = 1L;

    @Getter private String theme = "absolution";

    @Getter private String menuMode = "layout-menu-static";

    @Getter private String inputStyle = "outlined";

    @Getter private boolean orientationRTL;

    @Getter private List<FlatTheme> flatThemes = new ArrayList<>();

    @Getter private List<GradientTheme> gradientThemes = new ArrayList<>();

    @Getter private List<ImageTheme> imageThemes = new ArrayList<>();

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public void setMenuMode(String menuMode) {
        this.menuMode = menuMode;
    }

    public void setInputStyle(String inputStyle) {
        this.inputStyle = inputStyle;
    }

    public void setOrientationRTL(boolean orientationRTL) {
        this.orientationRTL = orientationRTL;
    }

    public String getInputStyleClass() {
        return "filled".equals(this.inputStyle) ? "ui-input-filled" : "";
    }

    @PostConstruct
    public void init() {
        flatThemes.add(new FlatTheme("Absolution", "absolution", "#628292"));
        flatThemes.add(new FlatTheme("Rebirth", "rebirth", "#007ad9"));
        flatThemes.add(new FlatTheme("Hope", "hope", "#67487e"));
        flatThemes.add(new FlatTheme("Bliss", "bliss", "#00b395"));
        flatThemes.add(new FlatTheme("Grace", "grace", "#5d2f92"));
        flatThemes.add(new FlatTheme("Dusk", "dusk", "#dd8400"));
        flatThemes.add(new FlatTheme("Navy", "navy", "#005a9e"));
        flatThemes.add(new FlatTheme("Infinity", "infinity", "#617e76"));
        flatThemes.add(new FlatTheme("Fate", "fate", "#0d5fa6"));
        flatThemes.add(new FlatTheme("Ruby", "ruby", "#ca5861"));
        flatThemes.add(new FlatTheme("Comfort", "comfort", "#0084a1"));

        gradientThemes.add(new GradientTheme("Faith", "faith", "#622774", "#c53364"));
        gradientThemes.add(new GradientTheme("Violet", "violet", "#5b247a", "#1bcedf"));
        gradientThemes.add(new GradientTheme("Honor", "honor", "#3bb2b8", "#00dac7"));
        gradientThemes.add(new GradientTheme("Rebel", "rebel", "#7367f0", "#ce9ffc"));
        gradientThemes.add(new GradientTheme("Vanity", "vanity", "#f76b1c", "#fad961"));
        gradientThemes.add(new GradientTheme("Valor", "valor", "#ff6b52", "#ff9851"));
        gradientThemes.add(new GradientTheme("Merit", "merit", "#1c4652", "#3d7b8a"));
        gradientThemes.add(new GradientTheme("Esprit", "esprit", "#276174", "#33c58e"));
        gradientThemes.add(new GradientTheme("Concord", "concord", "#5e2563", "#65799b"));
        gradientThemes.add(new GradientTheme("Dulce", "dulce", "#b3305f", "#ffaa85"));
        gradientThemes.add(new GradientTheme("Royal", "royal", "#171717", "#020202"));

        imageThemes.add(new ImageTheme("Hazel", "hazel", "hazel.jpg"));
        imageThemes.add(new ImageTheme("Essence", "essence", "essence.jpg"));
        imageThemes.add(new ImageTheme("Eternity", "eternity", "eternity.jpg"));
        imageThemes.add(new ImageTheme("Clarity", "clarity", "clarity.jpg"));
        imageThemes.add(new ImageTheme("Solace", "solace", "solace.jpg"));
        imageThemes.add(new ImageTheme("Joy", "joy", "joy.jpg"));
        imageThemes.add(new ImageTheme("Purity", "purity", "purity.jpg"));
        imageThemes.add(new ImageTheme("Euclid", "euclid", "euclid.jpg"));
        imageThemes.add(new ImageTheme("Elegance", "elegance", "elegance.jpg"));
        imageThemes.add(new ImageTheme("Tranquil", "tranquil", "tranquil.jpg"));
    }

    @Getter
    public static class FlatTheme implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String name;
        private final String file;
        private final String color;

        FlatTheme(String name, String file, String color) {
            this.name = name;
            this.file = file;
            this.color = color;
        }
    }

    @Getter
    public static class GradientTheme implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String name;
        private final String file;
        private final String color1;
        private final String color2;

        GradientTheme(String name, String file, String color1, String color2) {
            this.name = name;
            this.file = file;
            this.color1 = color1;
            this.color2 = color2;
        }
    }

    @Getter
    public static class ImageTheme implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String name;
        private final String file;
        private final String image;

        ImageTheme(String name, String file, String image) {
            this.name = name;
            this.file = file;
            this.image = image;
        }
    }
}
