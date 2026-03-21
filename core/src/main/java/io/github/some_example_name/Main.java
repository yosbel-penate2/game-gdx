package io.github.some_example_name;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer; // AÑADIR ESTA IMPORTACIÓN
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap; // AÑADIR ESTA IMPORTACIÓN
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable; // AÑADIR ESTA IMPORTACIÓN
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.controllers.ControllerAdapter;
import com.badlogic.gdx.controllers.PovDirection;

import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

public class Main extends ApplicationAdapter {
    Texture backgroundTexture;
    Texture bucketTexture;
    Texture dropTexture;
    Sound dropSound;
    Music music;

    SpriteBatch spriteBatch;
    FitViewport viewport;

    Stage stage;
    Skin skin;

    Sprite bucketSprite;

    Vector2 touchPos;

    Array<Sprite> dropSprites;

    float dropTimer;

    Rectangle bucketRectangle;
    Rectangle dropRectangle;

    float bucketJumpPower = 0.2f;
    float bucketJumpSpeed = 4f;
    float bucketTargetY = 0;
    float bucketCurrentY = 0;
    boolean isJumping = false;

    private int dropsCollected = 0;
    private BitmapFont font;
    private GlyphLayout glyphLayout;

    private float baseDropTimer = 1f;
    private float minDropTimer = 0.2f;
    private float difficultyScale = 0.95f;


@Override
public void create() {
    try {
        backgroundTexture = new Texture("background.png");
        bucketTexture = new Texture("bucket.png");
        dropTexture = new Texture("drop.png");

        dropSound = Gdx.audio.newSound(Gdx.files.internal("drop.mp3"));
        music = Gdx.audio.newMusic(Gdx.files.internal("music.mp3"));

        spriteBatch = new SpriteBatch();
        viewport = new FitViewport(8, 5);

        bucketSprite = new Sprite(bucketTexture);
        bucketSprite.setSize(1, 1);

        touchPos = new Vector2();
        dropSprites = new Array<>();
        glyphLayout = new GlyphLayout();
        bucketRectangle = new Rectangle();
        dropRectangle = new Rectangle();

        // Configurar controles - ESTO YA FUNCIONA POR SÍ SOLO
        Controllers.addListener(new ControllerAdapter() {
            @Override
            public boolean axisMoved(Controller controller, int axisIndex, float value) {
                if (axisIndex == 0 && Math.abs(value) > 0.2f) {
                    bucketSprite.translateX(value * 5f * Gdx.graphics.getDeltaTime());
                    return true;
                }
                return false;
            }

            @Override
            public boolean povMoved(Controller controller, int povIndex, PovDirection value) {
                if (povIndex == 0) {
                    if (value == PovDirection.west) 
                        bucketSprite.translateX(-4f * Gdx.graphics.getDeltaTime());
                    else if (value == PovDirection.east) 
                        bucketSprite.translateX(4f * Gdx.graphics.getDeltaTime());
                    return true;
                }
                return false;
            }

            @Override
            public boolean buttonDown(Controller controller, int buttonCode) {
                if (buttonCode == 0 || buttonCode == 1) {
                    startJump();
                    return true;
                }
                return false;
            }
        });

        music.setLooping(true);
        music.setVolume(.5f);
        music.play();

        bucketTargetY = 0;
        bucketCurrentY = 0;
        bucketSprite.setY(bucketCurrentY);

        // Configurar fuente
        if (Gdx.files.internal("fonts/arial.ttf").exists()) {
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/arial.ttf"));
            FreeTypeFontParameter parameter = new FreeTypeFontParameter();
            parameter.size = 24;
            parameter.color = Color.WHITE;
            parameter.borderWidth = 1f;
            parameter.borderColor = Color.BLACK;
            parameter.borderStraight = true;
            parameter.shadowColor = new Color(0f, 0f, 0f, 0.75f);
            font = generator.generateFont(parameter);
            generator.dispose();
        } else {
            Gdx.app.error("Font", "Fuente no encontrada, usando fuente por defecto");
            font = new BitmapFont();
            font.getData().setScale(1.5f);
        }
        
        // Configurar Stage y UI - CORREGIDO: No usar getInputListener()
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // Configurar Skin
        if (Gdx.files.internal("skin/uiskin.json").exists()) {
            skin = new Skin(Gdx.files.internal("skin/uiskin.json"));
        } else {
            skin = new Skin();
            Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
            skin.add("default", labelStyle);
        }

        // Asegurar que el drawable "white" existe
        if (skin.getDrawable("white") == null) {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.WHITE);
            pixmap.fill();
            Texture texture = new Texture(pixmap);
            TextureRegionDrawable drawable = new TextureRegionDrawable(texture);
            skin.add("white", drawable);
            pixmap.dispose();
        }

        // Crear HUD con fondo
        Table hudContainer = new Table();
        hudContainer.setFillParent(true);
        hudContainer.top().left();
        hudContainer.pad(10);

        Table indicatorBar = new Table();
        indicatorBar.setBackground(skin.newDrawable("white", new Color(0, 0, 0, 0.7f)));
        indicatorBar.pad(10, 15, 10, 15);

        Label dropsLabel = new Label("GOTAS: " + dropsCollected, skin);
        dropsLabel.setName("dropsLabel");

        Label difficultyLabel = new Label("DIFICULTAD: NORMAL", skin);
        difficultyLabel.setName("difficultyLabel");

        indicatorBar.add(dropsLabel).left();
        indicatorBar.add(difficultyLabel).padLeft(20);
        hudContainer.add(indicatorBar).left().top();
        stage.addActor(hudContainer);

        Gdx.app.log("Main", "Juego iniciado correctamente!");

    } catch (Exception e) {
        Gdx.app.error("Main", "Error al crear el juego", e);
        e.printStackTrace();
        Gdx.app.exit();
    }
}

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        if (stage != null) {
            stage.getViewport().update(width, height, true);
        }
    }

    @Override
    public void render() {
        input();
        logic();
        draw();
    }

    private void logic() {
        float worldWidth = viewport.getWorldWidth();
        float bucketWidth = bucketSprite.getWidth();
        float bucketHeight = bucketSprite.getHeight();

        bucketSprite.setX(MathUtils.clamp(bucketSprite.getX(), 0, worldWidth - bucketWidth));

        float delta = Gdx.graphics.getDeltaTime();

        // CORREGIDO: Usar bucketSprite.getY() en lugar de bucketRectangle.getY()
        bucketRectangle.set(bucketSprite.getX(), bucketSprite.getY(), bucketWidth, bucketHeight);

        // Animación del saltito
        if (isJumping) {
            bucketCurrentY += bucketJumpSpeed * delta;
            if (bucketCurrentY >= bucketTargetY + bucketJumpPower) {
                isJumping = false;
            }
        } else {
            bucketCurrentY -= bucketJumpSpeed * delta;
            if (bucketCurrentY <= bucketTargetY) {
                bucketCurrentY = bucketTargetY;
            }
        }
        bucketSprite.setY(bucketCurrentY);

        // Actualizar rectángulo de colisión después del salto
        bucketRectangle.set(bucketSprite.getX(), bucketSprite.getY(), bucketWidth, bucketHeight);

        // Procesar gotas
        for (int i = dropSprites.size - 1; i >= 0; i--) {
            Sprite dropSprite = dropSprites.get(i);
            float dropWidth = dropSprite.getWidth();
            float dropHeight = dropSprite.getHeight();

            dropSprite.translateY(-2f * delta);
            dropRectangle.set(dropSprite.getX(), dropSprite.getY(), dropWidth, dropHeight);

            if (dropSprite.getY() < -dropWidth) {
                dropSprites.removeIndex(i);
            } else if (bucketRectangle.overlaps(dropRectangle)) {
                dropSprites.removeIndex(i);
                if (dropSound != null) dropSound.play();
                dropsCollected++;
                startJump();
            }
        }

        // Crear nuevas gotas con dificultad dinámica
        dropTimer += delta;
        float currentDropInterval = calculateDropInterval();

        if (dropTimer > currentDropInterval) {
            dropTimer = 0;
            createDroplet();
        }
    }

    private float calculateDropInterval() {
        float interval = baseDropTimer * (float) Math.pow(difficultyScale, dropsCollected);
        return Math.max(minDropTimer, interval);
    }

    private void startJump() {
        if (!isJumping) {
            isJumping = true;
            bucketTargetY = bucketSprite.getY(); // CORREGIDO: Establecer target Y
            bucketCurrentY = bucketTargetY;
        }
    }

    private void input() {
        float speed = 4f;
        float delta = Gdx.graphics.getDeltaTime();

        // Controles de teclado
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            bucketSprite.translateX(speed * delta);
        } else if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            bucketSprite.translateX(-speed * delta);
        }

        // REMOVIDO: Controles duplicados de controller (ya están en ControllerAdapter)
        // El ControllerAdapter ya maneja los controles, no necesitas procesarlos aquí

        // Controles táctiles
        if (Gdx.input.isTouched()) {
            touchPos.set(Gdx.input.getX(), Gdx.input.getY());
            viewport.unproject(touchPos);
            bucketSprite.setCenterX(touchPos.x);
        }
    }

    private void draw() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        spriteBatch.setProjectionMatrix(viewport.getCamera().combined);
        spriteBatch.begin();

        if (backgroundTexture != null) {
            spriteBatch.draw(backgroundTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        }
        bucketSprite.draw(spriteBatch);
        for (Sprite dropSprite : dropSprites) {
            dropSprite.draw(spriteBatch);
        }

        spriteBatch.end();

        // Actualizar UI
        if (stage != null) {
            Label dropsLabel = stage.getRoot().findActor("dropsLabel");
            if (dropsLabel != null) {
                dropsLabel.setText("GOTAS: " + dropsCollected);
            }

            Label difficultyLabel = stage.getRoot().findActor("difficultyLabel");
            if (difficultyLabel != null) {
                float currentInterval = calculateDropInterval();
                String difficultyText;
                
                if (currentInterval <= 0.3f) difficultyText = "INSANE";
                else if (currentInterval <= 0.5f) difficultyText = "HARD";
                else if (currentInterval <= 0.7f) difficultyText = "MEDIUM";
                else if (currentInterval <= 0.85f) difficultyText = "EASY";
                else difficultyText = "NORMAL";
                
                difficultyLabel.setText("DIFICULTAD: " + difficultyText);
            }

            stage.act(Gdx.graphics.getDeltaTime());
            stage.draw();
        }
    }

    private void createDroplet() {
        float dropWidth = 1;
        float dropHeight = 1;
        float worldWidth = viewport.getWorldWidth();
        float worldHeight = viewport.getWorldHeight();

        Sprite dropSprite = new Sprite(dropTexture);
        dropSprite.setSize(dropWidth, dropHeight);
        dropSprite.setX(MathUtils.random(0f, worldWidth - dropWidth));
        dropSprite.setY(worldHeight);
        dropSprites.add(dropSprite);
    }

    @Override
    public void dispose() {
        if (font != null) font.dispose();
        if (spriteBatch != null) spriteBatch.dispose();
        if (backgroundTexture != null) backgroundTexture.dispose();
        if (bucketTexture != null) bucketTexture.dispose();
        if (dropTexture != null) dropTexture.dispose();
        if (dropSound != null) dropSound.dispose();
        if (music != null) music.dispose();
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
    }
}