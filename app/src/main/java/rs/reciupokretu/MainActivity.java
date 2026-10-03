package rs.reciupokretu;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Typeface;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int PICK_IMAGE = 1001;
    private static final int INK = Color.rgb(32, 69, 57);
    private static final int MUTED = Color.rgb(110, 128, 116);
    private static final int CREAM = Color.rgb(246, 245, 239);
    private static final int CORAL = Color.rgb(215, 104, 70);
    private static final int LINE = Color.rgb(224, 229, 218);

    private final ExecutorService imageExecutor = Executors.newFixedThreadPool(2);
    private WordStore store;
    private List<WordCard> cards;
    private List<WordFolder> folders;
    private List<WordScene> scenes;
    private String selectedLanguage;
    private String currentFolderId;
    private String currentSceneId;
    private AnimatorSet sceneAnimator;
    private String draftUri;
    private ImageView draftPreview;
    private Dialog formDialog;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new WordStore(this);
        cards = store.load();
        folders = store.loadFolders();
        scenes = WordScene.load(this);
        ensureSceneFolders();
        selectedLanguage = store.language();
        if (state != null) {
            String savedDraft = state.getString("draftUri");
            if (savedDraft != null) draftUri = savedDraft;
            currentFolderId = state.getString("currentFolderId");
            currentSceneId = state.getString("currentSceneId");
        }
        getWindow().setStatusBarColor(CREAM);
        getWindow().setNavigationBarColor(CREAM);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        renderCurrent();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        out.putString("draftUri", draftUri);
        out.putString("currentFolderId", currentFolderId);
        out.putString("currentSceneId", currentSceneId);
        super.onSaveInstanceState(out);
    }

    @Override
    protected void onDestroy() {
        stopSceneAnimation();
        imageExecutor.shutdownNow();
        super.onDestroy();
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(radius));
        return background;
    }

    private GradientDrawable outlined(int color, int radius) {
        GradientDrawable background = rounded(color, radius);
        background.setStroke(dp(1), LINE);
        return background;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private TextView action(String value, int backgroundColor, int textColor) {
        TextView view = text(value, 15, textColor, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(16), dp(13), dp(16), dp(13));
        view.setBackground(rounded(backgroundColor, 16));
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private void gap(LinearLayout parent, int height) {
        View spacer = new View(this);
        parent.addView(spacer, new LinearLayout.LayoutParams(1, dp(height)));
    }

    private LinearLayout vertical() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        return view;
    }

    private WordFolder findFolder(String id) {
        for (WordFolder folder : folders) if (folder.id.equals(id)) return folder;
        return null;
    }

    private WordScene findScene(String id) {
        for (WordScene scene : scenes) if (scene.id.equals(id)) return scene;
        return null;
    }

    private void ensureSceneFolders() {
        String[] ids = {"priroda", "skola", "kuhinja", "dnevna", "ostalo"};
        String[] names = {"Priroda", "Škola", "Kuhinja", "Dnevna soba", "Ostalo"};
        String[] symbols = {"🌿", "✏", "🍳", "▣", "✦"};
        List<WordFolder> updated = new ArrayList<>(folders);
        boolean changed = false;
        for (int i = 0; i < ids.length; i++) {
            if (findFolder(ids[i]) == null) {
                updated.add(new WordFolder(ids[i], names[i], symbols[i]));
                changed = true;
            }
        }
        if (changed && store.saveFolders(updated)) folders = updated;
    }

    private int countScenes(String folderId) {
        int count = 0;
        for (WordScene scene : scenes) if (scene.folderId.equals(folderId)) count++;
        return count;
    }

    private int countCards(String folderId) {
        int count = 0;
        for (WordCard card : cards) if (card.folderId.equals(folderId)) count++;
        return count;
    }

    private void renderCurrent() {
        WordScene scene = currentSceneId == null ? null : findScene(currentSceneId);
        if (scene != null) {
            renderScene(scene);
            return;
        }
        currentSceneId = null;
        WordFolder folder = currentFolderId == null ? null : findFolder(currentFolderId);
        if (folder == null) {
            currentFolderId = null;
            renderHome();
        } else {
            renderFolder(folder);
        }
    }

    private void renderHome() {
        stopSceneAnimation();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(CREAM);
        LinearLayout page = vertical();
        page.setPadding(dp(20), dp(28), dp(20), dp(34));
        scroll.addView(page);

        TextView eyebrow = text("MALA RADIONICA REČI  ✦", 12, CORAL, true);
        eyebrow.setLetterSpacing(0.12f);
        page.addView(eyebrow);
        gap(page, 10);
        page.addView(text("Reči u pokretu", 30, INK, true));
        gap(page, 7);
        page.addView(text("Izaberi mesto i istraži interaktivne scene.", 15, MUTED, false));
        gap(page, 24);

        addLanguageSelector(page);
        gap(page, 28);

        LinearLayout section = new LinearLayout(this);
        section.setGravity(Gravity.CENTER_VERTICAL);
        TextView sectionTitle = text("Izaberi folder", 20, INK, true);
        section.addView(sectionTitle, new LinearLayout.LayoutParams(0, -2, 1));
        TextView count = text("Ukupno: " + folders.size(), 13, MUTED, false);
        section.addView(count);
        page.addView(section);
        gap(page, 14);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        page.addView(grid);
        int tileWidth = (getResources().getDisplayMetrics().widthPixels - dp(52)) / 2;
        for (int i = 0; i < folders.size(); i++) {
            WordFolder folder = folders.get(i);
            View tile = createFolderTile(folder);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = tileWidth;
            params.height = -2;
            params.rightMargin = i % 2 == 0 ? dp(12) : 0;
            params.bottomMargin = dp(12);
            grid.addView(tile, params);
        }
        gap(page, 10);
        TextView addFolder = action("+  Dodaj novi folder", INK, Color.WHITE);
        page.addView(addFolder, new LinearLayout.LayoutParams(-1, -2));
        addFolder.setOnClickListener(v -> showFolderEditor(null));
        gap(page, 9);
        page.addView(text("Zadrži prst na folderu da ga preimenuješ ili obrišeš.", 12, MUTED, false));
        setContentView(scroll);
    }

    private View createFolderTile(WordFolder folder) {
        LinearLayout tile = vertical();
        tile.setPadding(dp(17), dp(18), dp(17), dp(17));
        tile.setBackground(rounded(Color.WHITE, 20));
        TextView symbol = text(folder.symbol, 36, CORAL, false);
        tile.addView(symbol);
        gap(tile, 15);
        tile.addView(text(folder.name, 18, INK, true));
        gap(tile, 5);
        int count = countScenes(folder.id);
        tile.addView(text(count + " scena" + (countCards(folder.id) > 0 ? " · " + countCards(folder.id) + " dodatih" : "") + "  ↗", 13, MUTED, false));
        tile.setOnClickListener(v -> {
            currentFolderId = folder.id;
            currentSceneId = null;
            renderCurrent();
        });
        tile.setOnLongClickListener(v -> {
            showFolderOptions(folder);
            return true;
        });
        return tile;
    }

    private void renderFolder(WordFolder folder) {
        stopSceneAnimation();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(CREAM);
        LinearLayout page = vertical();
        page.setPadding(dp(20), dp(24), dp(20), dp(34));
        scroll.addView(page);
        TextView back = text("←  Svi folderi", 15, CORAL, true);
        back.setPadding(0, dp(4), 0, dp(14));
        page.addView(back);
        back.setOnClickListener(v -> {
            currentFolderId = null;
            currentSceneId = null;
            renderHome();
        });
        page.addView(text(folder.symbol + "  " + folder.name, 29, INK, true));
        gap(page, 7);
        page.addView(text("Dodirni sliku da otkriješ reč.", 15, MUTED, false));
        gap(page, 23);
        addLanguageSelector(page);
        gap(page, 24);
        TextView add = action("+  Dodaj sliku u ovaj folder", INK, Color.WHITE);
        page.addView(add, new LinearLayout.LayoutParams(-1, -2));
        add.setOnClickListener(v -> showForm(null));
        gap(page, 25);
        int total = countScenes(folder.id);
        page.addView(text("Interaktivne scene  ·  " + total, 20, INK, true));
        gap(page, 14);
        if (total > 0) {
            GridLayout grid = new GridLayout(this);
            grid.setColumnCount(2);
            page.addView(grid);
            int tileWidth = (getResources().getDisplayMetrics().widthPixels - dp(52)) / 2;
            int shown = 0;
            for (WordScene scene : scenes) {
                if (!folder.id.equals(scene.folderId)) continue;
                View tile = createSceneTile(scene);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = tileWidth;
                params.height = -2;
                params.rightMargin = shown % 2 == 0 ? dp(12) : 0;
                params.bottomMargin = dp(12);
                grid.addView(tile, params);
                shown++;
            }
        }
        int customCount = countCards(folder.id);
        gap(page, 15);
        page.addView(text("Moje kartice  ·  " + customCount, 20, INK, true));
        gap(page, 14);
        if (customCount == 0) {
            showEmptyState(page);
        } else {
            GridLayout grid = new GridLayout(this);
            grid.setColumnCount(2);
            page.addView(grid);
            int tileWidth = (getResources().getDisplayMetrics().widthPixels - dp(52)) / 2;
            int shown = 0;
            for (WordCard card : cards) {
                if (!folder.id.equals(card.folderId)) continue;
                View tile = createTile(card, shown + 1);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = tileWidth;
                params.height = -2;
                params.rightMargin = shown % 2 == 0 ? dp(12) : 0;
                params.bottomMargin = dp(12);
                grid.addView(tile, params);
                shown++;
            }
        }
        setContentView(scroll);
    }

    private void addLanguageSelector(LinearLayout page) {
        page.addView(text("Jezik za učenje", 17, INK, true));
        gap(page, 10);
        LinearLayout languages = new LinearLayout(this);
        languages.setOrientation(LinearLayout.HORIZONTAL);
        page.addView(languages);
        addLanguageChip(languages, "Srpski", "sr");
        addLanguageChip(languages, "English", "en");
        addLanguageChip(languages, "Русский", "ru");
    }

    private void showFolderOptions(WordFolder folder) {
        new AlertDialog.Builder(this)
                .setTitle(folder.name)
                .setItems(new String[]{"Preimenuj", "Obriši"}, (dialog, which) -> {
                    if (which == 0) {
                        showFolderEditor(folder);
                    } else if (countScenes(folder.id) > 0) {
                        toast("Folder sa ugrađenim scenama ne može da se obriše.");
                    } else if (countCards(folder.id) > 0) {
                        toast("Prvo premesti ili obriši kartice iz ovog foldera.");
                    } else {
                        new AlertDialog.Builder(this)
                                .setTitle("Obrisati folder?")
                                .setMessage(folder.name + " će biti uklonjen.")
                                .setNegativeButton("Otkaži", null)
                                .setPositiveButton("Obriši", (confirmation, choice) -> {
                                    List<WordFolder> updated = new ArrayList<>(folders);
                                    updated.removeIf(item -> item.id.equals(folder.id));
                                    if (store.saveFolders(updated)) {
                                        folders = updated;
                                        renderHome();
                                    } else {
                                        toast("Folder nije obrisan. Pokušaj ponovo.");
                                    }
                                }).show();
                    }
                }).show();
    }

    private void showFolderEditor(WordFolder existing) {
        EditText name = input("Naziv foldera", existing == null ? "" : existing.name);
        LinearLayout container = vertical();
        container.setPadding(dp(22), dp(8), dp(22), 0);
        container.addView(name, new LinearLayout.LayoutParams(-1, -2));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Novi folder" : "Preimenuj folder")
                .setView(container)
                .setNegativeButton("Otkaži", null)
                .setPositiveButton("Sačuvaj", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = name.getText().toString().trim();
            if (title.isEmpty()) {
                name.setError("Upiši naziv foldera.");
                return;
            }
            for (WordFolder folder : folders) {
                if ((existing == null || !folder.id.equals(existing.id))
                        && folder.name.toLowerCase(Locale.ROOT).equals(title.toLowerCase(Locale.ROOT))) {
                    name.setError("Folder sa tim nazivom već postoji.");
                    return;
                }
            }
            WordFolder changed = existing == null
                    ? new WordFolder(UUID.randomUUID().toString(), title, "✦")
                    : new WordFolder(existing.id, title, existing.symbol);
            List<WordFolder> updated = new ArrayList<>(folders);
            if (existing == null) {
                updated.add(changed);
            } else {
                for (int i = 0; i < updated.size(); i++) {
                    if (updated.get(i).id.equals(existing.id)) {
                        updated.set(i, changed);
                        break;
                    }
                }
            }
            if (store.saveFolders(updated)) {
                folders = updated;
                dialog.dismiss();
                renderHome();
            } else {
                toast("Folder nije sačuvan. Pokušaj ponovo.");
            }
        }));
        dialog.show();
    }

    private void addLanguageChip(LinearLayout row, String name, String code) {
        boolean selected = selectedLanguage.equals(code);
        TextView chip = text(name, 13, selected ? Color.WHITE : INK, selected);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(10), dp(12), dp(10), dp(12));
        chip.setBackground(selected ? rounded(INK, 14) : outlined(Color.WHITE, 14));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
        params.rightMargin = code.equals("ru") ? 0 : dp(8);
        row.addView(chip, params);
        chip.setOnClickListener(v -> {
            selectedLanguage = code;
            store.setLanguage(code);
            renderCurrent();
        });
    }

    @Override
    public void onBackPressed() {
        if (currentSceneId != null) {
            currentSceneId = null;
            renderCurrent();
        } else if (currentFolderId != null) {
            currentFolderId = null;
            renderHome();
        } else {
            super.onBackPressed();
        }
    }

    private void showEmptyState(LinearLayout page) {
        LinearLayout empty = vertical();
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(26), dp(42), dp(26), dp(42));
        empty.setBackground(outlined(Color.WHITE, 24));
        page.addView(empty, new LinearLayout.LayoutParams(-1, -2));
        TextView symbol = text("▣", 54, CORAL, false);
        empty.addView(symbol);
        gap(empty, 8);
        empty.addView(text("Prva reč čeka svoju sliku", 19, INK, true));
        gap(empty, 8);
        TextView explanation = text("Dodaj GIF ili animirani WebP i upiši istu reč na sva tri jezika.", 14, MUTED, false);
        explanation.setGravity(Gravity.CENTER);
        empty.addView(explanation);
    }

    private View createTile(WordCard card, int number) {
        LinearLayout tile = vertical();
        tile.setBackground(rounded(Color.WHITE, 20));
        tile.setClipToOutline(true);
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackgroundColor(Color.rgb(230, 236, 227));
        image.setContentDescription("Animirana slika za karticu " + number);
        tile.addView(image, new LinearLayout.LayoutParams(-1, dp(148)));
        loadImage(image, card.imageUri);
        LinearLayout caption = vertical();
        caption.setPadding(dp(13), dp(11), dp(13), dp(13));
        tile.addView(caption);
        TextView numberLabel = text(String.format(java.util.Locale.ROOT, "KARTICA %02d", number), 11, MUTED, true);
        numberLabel.setLetterSpacing(0.1f);
        caption.addView(numberLabel);
        gap(caption, 5);
        caption.addView(text("Dodirni za reč  ↗", 14, INK, true));
        tile.setOnClickListener(v -> showWord(card));
        return tile;
    }

    private View createSceneTile(WordScene scene) {
        LinearLayout tile = vertical();
        tile.setBackground(rounded(Color.WHITE, 20));
        tile.setClipToOutline(true);
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackgroundColor(Color.rgb(230, 236, 227));
        image.setContentDescription("Scena: " + scene.title);
        tile.addView(image, new LinearLayout.LayoutParams(-1, dp(148)));
        loadImage(image, "asset:" + scene.imageAsset);
        LinearLayout caption = vertical();
        caption.setPadding(dp(13), dp(11), dp(13), dp(13));
        tile.addView(caption);
        caption.addView(text(scene.title, 16, INK, true));
        gap(caption, 4);
        caption.addView(text("3 predmeta  ·  istraži ↗", 12, MUTED, false));
        tile.setOnClickListener(v -> {
            currentSceneId = scene.id;
            renderCurrent();
        });
        return tile;
    }

    private void renderScene(WordScene scene) {
        stopSceneAnimation();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(CREAM);
        LinearLayout page = vertical();
        page.setPadding(dp(20), dp(24), dp(20), dp(35));
        scroll.addView(page);
        WordFolder folder = findFolder(scene.folderId);
        TextView back = text("←  " + (folder == null ? "Folder" : folder.name), 15, CORAL, true);
        back.setPadding(0, dp(4), 0, dp(14));
        page.addView(back);
        back.setOnClickListener(v -> {
            currentSceneId = null;
            renderCurrent();
        });
        page.addView(text(scene.title, 29, INK, true));
        gap(page, 7);
        page.addView(text("Dodirni označeni predmet na slici.", 15, MUTED, false));
        gap(page, 18);

        int frameWidth = getResources().getDisplayMetrics().widthPixels - dp(40);
        int frameHeight = Math.round(frameWidth * 2f / 3f);
        try (InputStream input = getAssets().open(scene.imageAsset)) {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, bounds);
            if (bounds.outWidth > 0 && bounds.outHeight > 0) {
                frameHeight = Math.round(frameWidth * (float) bounds.outHeight / bounds.outWidth);
            }
        } catch (IOException ignored) { }

        FrameLayout frame = new FrameLayout(this) {
            @Override
            public boolean performClick() {
                super.performClick();
                return true;
            }
        };
        frame.setBackground(rounded(Color.WHITE, 18));
        frame.setClipToOutline(true);
        frame.setContentDescription("Interaktivna slika: " + scene.title);
        page.addView(frame, new LinearLayout.LayoutParams(-1, frameHeight));
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.FIT_XY);
        frame.addView(image, new FrameLayout.LayoutParams(-1, -1));
        loadImage(image, "asset:" + scene.imageAsset);

        gap(page, 22);
        TextView languageLabel = text("REČ NA IZABRANOM JEZIKU", 12, CORAL, true);
        languageLabel.setLetterSpacing(0.09f);
        page.addView(languageLabel);
        gap(page, 7);
        TextView revealed = text("Dodirni predmet", 29, INK, true);
        page.addView(revealed);
        gap(page, 19);
        final WordScene.Hotspot[] selected = {null};
        LinearLayout languageRow = new LinearLayout(this);
        page.addView(languageRow);
        addSceneLanguageChip(languageRow, revealed, selected, "Srpski", "sr");
        addSceneLanguageChip(languageRow, revealed, selected, "English", "en");
        addSceneLanguageChip(languageRow, revealed, selected, "Русский", "ru");

        for (WordScene.Hotspot hotspot : scene.hotspots) {
            TextView marker = text("+", 16, CORAL, true);
            marker.setGravity(Gravity.CENTER);
            GradientDrawable markerBackground = rounded(Color.argb(205, 255, 255, 255), 24);
            markerBackground.setStroke(dp(2), CORAL);
            marker.setBackground(markerBackground);
            marker.setContentDescription("Pokaži: " + hotspot.sr);
            FrameLayout.LayoutParams markerParams = new FrameLayout.LayoutParams(dp(28), dp(28));
            markerParams.leftMargin = Math.max(0, Math.min(frameWidth - dp(28),
                    Math.round(frameWidth * hotspot.centerX()) - dp(14)));
            markerParams.topMargin = Math.max(0, Math.min(frameHeight - dp(28),
                    Math.round(frameHeight * hotspot.centerY()) - dp(14)));
            frame.addView(marker, markerParams);
            marker.setOnClickListener(v -> revealHotspot(hotspot, selected, revealed));
        }
        frame.setOnClickListener(v -> { });
        frame.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                v.performClick();
                float x = event.getX() / v.getWidth();
                float y = event.getY() / v.getHeight();
                WordScene.Hotspot best = null;
                for (WordScene.Hotspot hotspot : scene.hotspots) {
                    if (hotspot.contains(x, y) && (best == null || hotspot.area() < best.area())) best = hotspot;
                }
                if (best == null) {
                    toast("Dodirni jedan od označenih predmeta.");
                } else {
                    revealHotspot(best, selected, revealed);
                }
                return true;
            }
            return true;
        });

        ObjectAnimator zoomX = ObjectAnimator.ofFloat(frame, View.SCALE_X, 1f, 1.035f, 1f);
        ObjectAnimator zoomY = ObjectAnimator.ofFloat(frame, View.SCALE_Y, 1f, 1.035f, 1f);
        zoomX.setDuration(8500);
        zoomY.setDuration(8500);
        zoomX.setRepeatCount(ValueAnimator.INFINITE);
        zoomY.setRepeatCount(ValueAnimator.INFINITE);
        zoomX.setInterpolator(new AccelerateDecelerateInterpolator());
        zoomY.setInterpolator(new AccelerateDecelerateInterpolator());
        sceneAnimator = new AnimatorSet();
        sceneAnimator.playTogether(zoomX, zoomY);
        sceneAnimator.start();
        setContentView(scroll);
    }

    private void revealHotspot(WordScene.Hotspot hotspot, WordScene.Hotspot[] selected, TextView revealed) {
        selected[0] = hotspot;
        revealed.setText(hotspot.word(selectedLanguage));
        revealed.setAlpha(0.45f);
        revealed.animate().alpha(1f).setDuration(260).start();
    }

    private void addSceneLanguageChip(LinearLayout row, TextView revealed,
                                      WordScene.Hotspot[] selected, String name, String code) {
        boolean active = selectedLanguage.equals(code);
        TextView chip = text(name, 13, active ? Color.WHITE : INK, active);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(6), dp(12), dp(6), dp(12));
        chip.setBackground(active ? rounded(INK, 14) : outlined(Color.WHITE, 14));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
        params.rightMargin = code.equals("ru") ? 0 : dp(8);
        row.addView(chip, params);
        chip.setOnClickListener(v -> {
            selectedLanguage = code;
            store.setLanguage(code);
            if (selected[0] != null) revealed.setText(selected[0].word(code));
            for (int i = 0; i < row.getChildCount(); i++) {
                TextView item = (TextView) row.getChildAt(i);
                boolean selectedItem = item == chip;
                item.setBackground(selectedItem ? rounded(INK, 14) : outlined(Color.WHITE, 14));
                item.setTextColor(selectedItem ? Color.WHITE : INK);
            }
        });
    }

    private void stopSceneAnimation() {
        if (sceneAnimator != null) {
            sceneAnimator.cancel();
            sceneAnimator = null;
        }
    }

    private void loadImage(ImageView target, String uriText) {
        target.setTag(uriText);
        imageExecutor.execute(() -> {
            try {
                ImageDecoder.Source source = uriText.startsWith("asset:")
                        ? ImageDecoder.createSource(getAssets(), uriText.substring(6))
                        : ImageDecoder.createSource(getContentResolver(), Uri.parse(uriText));
                Drawable drawable = ImageDecoder.decodeDrawable(source, (decoder, info, source1) -> {
                    int width = info.getSize().getWidth();
                    int height = info.getSize().getHeight();
                    double scale = Math.min(1.0, 900.0 / Math.max(width, height));
                    decoder.setTargetSize(Math.max(1, (int) (width * scale)), Math.max(1, (int) (height * scale)));
                });
                runOnUiThread(() -> {
                    if (uriText.equals(target.getTag())) {
                        target.setImageDrawable(drawable);
                        if (drawable instanceof AnimatedImageDrawable) ((AnimatedImageDrawable) drawable).start();
                    }
                });
            } catch (IOException | RuntimeException error) {
                runOnUiThread(() -> {
                    if (uriText.equals(target.getTag())) target.setContentDescription("Slika nije dostupna");
                });
            }
        });
    }

    private Dialog bottomDialog(View content) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(content);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(rounded(CREAM, 24));
            window.setLayout(-1, -2);
            window.setGravity(Gravity.BOTTOM);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.dimAmount = 0.45f;
            window.setAttributes(attributes);
        }
        dialog.show();
        if (window != null) window.setLayout(-1, -2);
        return dialog;
    }

    private void showWord(WordCard card) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout sheet = vertical();
        sheet.setPadding(dp(22), dp(20), dp(22), dp(28));
        scroll.addView(sheet);
        TextView overline = text("POGLEDAJ I IZGOVORI", 12, CORAL, true);
        overline.setLetterSpacing(0.1f);
        sheet.addView(overline);
        gap(sheet, 15);
        ImageView hero = new ImageView(this);
        hero.setScaleType(ImageView.ScaleType.FIT_CENTER);
        hero.setBackground(rounded(Color.WHITE, 18));
        hero.setClipToOutline(true);
        sheet.addView(hero, new LinearLayout.LayoutParams(-1, dp(230)));
        loadImage(hero, card.imageUri);
        gap(sheet, 22);
        TextView word = text(card.word(selectedLanguage), 36, INK, true);
        word.setGravity(Gravity.CENTER);
        sheet.addView(word);
        gap(sheet, 19);
        LinearLayout languages = new LinearLayout(this);
        sheet.addView(languages);
        addDetailChip(languages, word, card, "Srpski", "sr");
        addDetailChip(languages, word, card, "English", "en");
        addDetailChip(languages, word, card, "Русский", "ru");
        gap(sheet, 23);
        LinearLayout controls = new LinearLayout(this);
        sheet.addView(controls);
        TextView edit = action("Izmeni", Color.WHITE, INK);
        controls.addView(edit, new LinearLayout.LayoutParams(0, -2, 1));
        TextView delete = action("Obriši", Color.WHITE, CORAL);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(0, -2, 1);
        deleteParams.leftMargin = dp(10);
        controls.addView(delete, deleteParams);
        Dialog dialog = bottomDialog(scroll);
        edit.setOnClickListener(v -> {
            dialog.dismiss();
            showForm(card);
        });
        delete.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Obrisati karticu?")
                .setMessage("Slika i njene reči biće uklonjene iz aplikacije.")
                .setNegativeButton("Otkaži", null)
                .setPositiveButton("Obriši", (d, which) -> {
                    List<WordCard> updated = new ArrayList<>(cards);
                    updated.removeIf(item -> item.id.equals(card.id));
                    if (store.save(updated)) {
                        cards = updated;
                        dialog.dismiss();
                        renderCurrent();
                    } else {
                        toast("Kartica nije obrisana. Pokušaj ponovo.");
                    }
                }).show());
    }

    private void addDetailChip(LinearLayout row, TextView word, WordCard card, String name, String code) {
        TextView chip = text(name, 13, INK, true);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(6), dp(12), dp(6), dp(12));
        chip.setBackground(outlined(Color.WHITE, 14));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
        params.rightMargin = code.equals("ru") ? 0 : dp(8);
        row.addView(chip, params);
        chip.setOnClickListener(v -> {
            selectedLanguage = code;
            store.setLanguage(code);
            word.setText(card.word(code));
            for (int i = 0; i < row.getChildCount(); i++) {
                TextView item = (TextView) row.getChildAt(i);
                boolean active = item == chip;
                item.setBackground(active ? rounded(INK, 14) : outlined(Color.WHITE, 14));
                item.setTextColor(active ? Color.WHITE : INK);
            }
        });
        if (selectedLanguage.equals(code)) {
            chip.setBackground(rounded(INK, 14));
            chip.setTextColor(Color.WHITE);
        }
    }

    private EditText input(String hint, String value) {
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setTextSize(17);
        field.setTextColor(INK);
        field.setHintTextColor(MUTED);
        field.setHint(hint);
        field.setText(value);
        field.setPadding(dp(14), dp(12), dp(14), dp(12));
        field.setBackground(outlined(Color.WHITE, 13));
        return field;
    }

    private void showForm(WordCard existing) {
        draftUri = existing == null ? null : existing.imageUri;
        ScrollView scroll = new ScrollView(this);
        LinearLayout sheet = vertical();
        sheet.setPadding(dp(22), dp(20), dp(22), dp(28));
        scroll.addView(sheet);
        sheet.addView(text(existing == null ? "Nova kartica" : "Izmeni karticu", 26, INK, true));
        gap(sheet, 5);
        sheet.addView(text("Jedna slika, tri jezika.", 14, MUTED, false));
        gap(sheet, 17);
        sheet.addView(text("Folder", 13, INK, true));
        gap(sheet, 6);
        List<String> folderNames = new ArrayList<>();
        for (WordFolder folder : folders) folderNames.add(folder.name);
        Spinner folderPicker = new Spinner(this);
        ArrayAdapter<String> folderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, folderNames);
        folderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        folderPicker.setAdapter(folderAdapter);
        folderPicker.setPadding(dp(10), dp(5), dp(10), dp(5));
        sheet.addView(folderPicker, new LinearLayout.LayoutParams(-1, dp(52)));
        String initialFolderId = existing == null ? currentFolderId : existing.folderId;
        for (int i = 0; i < folders.size(); i++) {
            if (folders.get(i).id.equals(initialFolderId)) {
                folderPicker.setSelection(i);
                break;
            }
        }
        gap(sheet, 16);
        draftPreview = new ImageView(this);
        draftPreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        draftPreview.setBackground(rounded(Color.WHITE, 18));
        draftPreview.setClipToOutline(true);
        sheet.addView(draftPreview, new LinearLayout.LayoutParams(-1, dp(150)));
        if (draftUri != null) loadImage(draftPreview, draftUri);
        gap(sheet, 10);
        TextView choose = action("Izaberi GIF ili animirani WebP", Color.WHITE, INK);
        sheet.addView(choose);
        choose.setOnClickListener(v -> pickImage());
        gap(sheet, 18);
        sheet.addView(text("Srpski", 13, INK, true));
        gap(sheet, 6);
        EditText sr = input("npr. jabuka", existing == null ? "" : existing.sr);
        sheet.addView(sr, new LinearLayout.LayoutParams(-1, -2));
        gap(sheet, 12);
        sheet.addView(text("English", 13, INK, true));
        gap(sheet, 6);
        EditText en = input("e.g. apple", existing == null ? "" : existing.en);
        sheet.addView(en, new LinearLayout.LayoutParams(-1, -2));
        gap(sheet, 12);
        sheet.addView(text("Русский", 13, INK, true));
        gap(sheet, 6);
        EditText ru = input("например, яблоко", existing == null ? "" : existing.ru);
        sheet.addView(ru, new LinearLayout.LayoutParams(-1, -2));
        gap(sheet, 20);
        TextView save = action("Sačuvaj karticu", INK, Color.WHITE);
        sheet.addView(save);
        formDialog = bottomDialog(scroll);
        formDialog.setOnDismissListener(dialog -> {
            formDialog = null;
            draftPreview = null;
            draftUri = null;
        });
        save.setOnClickListener(v -> {
            String srValue = sr.getText().toString().trim();
            String enValue = en.getText().toString().trim();
            String ruValue = ru.getText().toString().trim();
            if (draftUri == null) {
                toast("Prvo izaberi animiranu sliku.");
                return;
            }
            if (srValue.isEmpty() || enValue.isEmpty() || ruValue.isEmpty()) {
                toast("Upiši reč na sva tri jezika.");
                return;
            }
            int folderIndex = folderPicker.getSelectedItemPosition();
            if (folderIndex < 0 || folderIndex >= folders.size()) {
                toast("Izaberi folder za ovu karticu.");
                return;
            }
            String folderId = folders.get(folderIndex).id;
            WordCard result = new WordCard(existing == null ? UUID.randomUUID().toString() : existing.id,
                    draftUri, folderId, srValue, enValue, ruValue);
            List<WordCard> updated = new ArrayList<>(cards);
            if (existing == null) {
                updated.add(0, result);
            } else {
                for (int i = 0; i < updated.size(); i++) {
                    if (updated.get(i).id.equals(existing.id)) {
                        updated.set(i, result);
                        break;
                    }
                }
            }
            if (store.save(updated)) {
                cards = updated;
                currentFolderId = folderId;
                formDialog.dismiss();
                renderCurrent();
            } else {
                toast("Kartica nije sačuvana. Pokušaj ponovo.");
            }
        });
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_IMAGE || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            draftUri = uri.toString();
            if (draftPreview != null) loadImage(draftPreview, draftUri);
        } catch (SecurityException error) {
            toast("Nije moguće zadržati pristup ovoj slici. Izaberi drugu.");
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
