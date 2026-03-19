package io.github.some_example_name;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    Texture backgroundTexture;
    Texture bucketTexture;
    Texture dropTexture;
    Sound dropSound;
    Music music;

    SpriteBatch spriteBatch;
    FitViewport viewport;

    Sprite bucketSprite;

    Vector2 touchPos;

    Array<Sprite> dropSprites;

    float dropTimer;

    Rectangle bucketRectangle;
    Rectangle dropRectangle;

    float bucketJumpPower = 0.2f; // Cuánto sube la cubeta
    float bucketJumpSpeed = 4f;   // Velocidad del salto
    float bucketTargetY = 0;      // Posición Y base
    float bucketCurrentY = 0;     // Posición actual (para animación)
    boolean isJumping = false;    // Indica si está saltando

    private int dropsCollected = 0; // Contador de gotas recogidas
    private BitmapFont font;         // Fuente para mostrar el texto

    @Override
    public void create() {
        try {
            backgroundTexture = new Texture("background.png");
            bucketTexture = new Texture("bucket.png");
            dropTexture = new Texture("drop.png");

            dropSound=Gdx.audio.newSound(Gdx.files.internal("drop.mp3"));
            music=Gdx.audio.newMusic(Gdx.files.internal("music.mp3"));

            spriteBatch=new SpriteBatch();
            viewport=new FitViewport(8,5);

            bucketSprite=new Sprite(bucketTexture);
            bucketSprite.setSize(1,1);

            touchPos=new Vector2();
            dropSprites=new Array<>();

            bucketRectangle=new Rectangle();
            dropRectangle=new Rectangle();

            music.setLooping(true);
            music.setVolume(.5f);
            music.play();

            bucketSprite.setSize(1, 1);
            bucketTargetY = 0; // Suponiendo que empieza en Y=0
            bucketCurrentY = 0;
            bucketSprite.setY(bucketCurrentY);

            // --- NUEVA FUENTE CON FREETYPE ---
            if (Gdx.files.internal("fonts/arial.ttf").exists()) {
                FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/arial.ttf"));
                FreeTypeFontParameter parameter = new FreeTypeFontParameter();
                parameter.size = 6; // Tamaño en píxeles (ya es pequeño)
                parameter.color = Color.WHITE;
                parameter.borderWidth = 1f; // Borde opcional para legibilidad
                parameter.borderColor = Color.BLACK; // Mejora visibilidad sobre fondos
                parameter.borderStraight = true;

                parameter.shadowColor = new Color(0f, 0f, 0f, 0.75f);
                font = generator.generateFont(parameter);
                generator.dispose();
            } else {
                Gdx.app.error("Font", "Fuente 'fonts/arial.ttf' no encontrada, usando fuente por defecto");
                font = new BitmapFont(); // Fallback
                font.getData().setScale(0.15f); // Escala muy pequeña para BitmapFont
            }
            // ------------------------------------
            // No se añade listener de gdx-controllers porque la dependencia no está disponible para 1.13.1.

        } catch (Exception e) {
            Gdx.app.error("Main", "Error al crear el juego", e);
            Gdx.app.exit();
        }

    }
    @Override
    public void resize(int width, int height){
        viewport.update(width, height,  true);
    }

    @Override
    public void render() {
        input();
        logic();
        draw();
    }

    private void logic() {
        float worldWidth=viewport.getWorldWidth();

        float bucketWidth = bucketSprite.getWidth();
        float bucketHeight= bucketSprite.getHeight();

        bucketSprite.setX(MathUtils.clamp(bucketSprite.getX(),0, worldWidth-bucketWidth));

        float delta=Gdx.graphics.getDeltaTime();

        bucketRectangle.set(bucketSprite.getX(), bucketRectangle.getY(), bucketWidth, bucketHeight);


        // Animación del saltito
        if (isJumping) {
            bucketCurrentY += bucketJumpSpeed * Gdx.graphics.getDeltaTime();
            if (bucketCurrentY >= bucketTargetY + bucketJumpPower) {
                isJumping = false;
            }
        } else {
            bucketCurrentY -= bucketJumpSpeed * Gdx.graphics.getDeltaTime();
            if (bucketCurrentY <= bucketTargetY) {
                bucketCurrentY = bucketTargetY;
            }
        }
        bucketSprite.setY(bucketCurrentY);

        // Actualizar rectángulo de colisión
        bucketRectangle.set(bucketSprite.getX(), bucketSprite.getY(), bucketWidth, bucketHeight);


        for (int i = dropSprites.size -1; i>=0; i--){
            Sprite dropSprite=dropSprites.get(i);
            float dropWidth=dropSprite.getWidth();
            float dropHeight=dropSprite.getHeight();

            dropSprite.translateY((-2f*delta));
            dropRectangle.set(dropSprite.getX(), dropSprite.getY(), dropWidth, dropHeight);

            if (dropSprite.getY()< -dropWidth) {
                dropSprites.removeIndex(i);
            }else if (bucketRectangle.overlaps(dropRectangle)) {
                dropSprites.removeIndex(i);
                dropSound.play();
                dropsCollected++;
                startJump(); // ¡Saltito aquí!
            }
        }

        dropTimer+=delta;
        if (dropTimer > 1f) {
            dropTimer=0;
            createDroplet();
        }

    }

    private void startJump() {
        isJumping = true;
    }

    private void input() {
        float speed=4f;
        float delta=Gdx.graphics.getDeltaTime();

        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)){
            bucketSprite.translateX(speed*delta);
        }else if (Gdx.input.isKeyPressed(Input.Keys.LEFT)){
            bucketSprite.translateX(-speed*delta);
        }

        if (Gdx.input.isTouched()) {
            touchPos.set(Gdx.input.getX(), Gdx.input.getY());
            viewport.unproject(touchPos);
            bucketSprite.setCenterX(touchPos.x);
        }
    }

    private void draw() {
        ScreenUtils.clear(Color.BLACK);
        viewport.apply();
        spriteBatch.setProjectionMatrix(viewport.getCamera().combined);
        spriteBatch.begin();

        float worldWidth = viewport.getWorldWidth();
        float worldHeight=viewport.getWorldHeight();

        spriteBatch.draw(backgroundTexture, 0,0, worldWidth, worldHeight);
        bucketSprite.draw(spriteBatch);

        for (Sprite dropSprite: dropSprites
             ) {
            dropSprite.draw(spriteBatch);
        }


        // Dibujar el contador encima de todo
        font.setColor(Color.WHITE);
        font.getData().setScale(0.1f);
        font.draw(
            spriteBatch,
            "GOTAS: " + dropsCollected,
            0.2f,  // margen izquierdo
            worldHeight - 0.2f  // desde arriba
        );

        spriteBatch.end();
    }

    private void createDroplet(){
        float dropWidth=1;
        float dropHeight=1;
        float worldWidth=viewport.getWorldWidth();
        float worldHeight=viewport.getWorldHeight();

        Sprite dropSprite=new Sprite(dropTexture);
        dropSprite.setSize(dropWidth, dropHeight);
        dropSprite.setX(MathUtils.random(0f, worldWidth-dropWidth));
        dropSprite.setY(worldHeight);
        dropSprites.add(dropSprite);

    }

    @Override
    public void dispose() {
        font.dispose();
    }
}
