package code;

/*
* Kaitlyn Morris
* Honors Project/Final for CS 465
* Solor Syatem Visualization
*/

//Imports 
import java.nio.*;
import java.lang.Math;
import javax.swing.*;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.util.*;
import com.jogamp.common.nio.Buffers;
import org.joml.*;
import java.awt.event.*;
import java.awt.*;

public class Code extends JFrame implements GLEventListener, KeyListener {
   
   private GLJPanel myPanel;                    
   private FPSAnimator animator;                
   private int renderingProgram1;  //Shadow pass shader program
   private int renderingProgram2;  //Main render shader program
   private int[] vao = new int[1];              
   private int[] vbo = new int[15];//Vertex Buffer Objects (sphere, orbits, rings)
   private Sphere mySphere;                     
   private int numSphereVerts, numOrbitVerts;   
   
   //Shadow Mapping 
   private int shadowTex, shadowBuffer;
   private int shadowMVPLoc, lightPosLoc;
   private int useLightingLoc, useShadowsLoc;         
   private Matrix4f lightVmat = new Matrix4f(); //Light's view matrix
   private Matrix4f lightPmat = new Matrix4f(); //Light's projection matrix
   
   //Camera & Projection
   private float aspect;                        
   private int mvLoc, pLoc, colorLoc; //Shader uniform locations
   private int useTextureLoc, normLoc;  
   private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);  //Matrix transfer buffer
   private Matrix4fStack mvStack = new Matrix4fStack(10);        //Model-view matrix stack
   private Matrix4f pMat = new Matrix4f(); //Projection matrix
   private Matrix4f vMat = new Matrix4f(); //View matrix
   private boolean projectionNeedsUpdate = true;
   
   //Time Variables
   private long startTime, stageStartTime;      
   private boolean timeStopped = false;         
   private float frozenTime = 0.0f;             
   
   //Navagation
   private int currentStage = -1;               //Current planet index
   private boolean isTransitioning = false;     //Fly-by
   private boolean topDownMode = true;          //Toggle between modes
   private boolean scaledViewMode = false;      //Toggle between views
   private boolean returningToSun = false;      //Pluto to Sun navagation 
   private Vector3f lockedPlanetPos = new Vector3f();  //Target planet position
   private Vector3f currentCamPos = new Vector3f();    //Current camera position
   
   //Scene Data 
   private CelestialBody[] planets;             
   private int sunTexture, skydomeTexture;      
   private int asteroidTexture, saturnRingTexture;
   private JLabel infoLabel, planetInfoLabel;   
   
   //-------------------------------------------------------------------------------
   
   //Class to Hold Object Information
   private static class CelestialBody {
      String name, description;
      float orbitRadius;      
      float size;             
      float rotationSpeed;    
      float orbitSpeed;      
      int texture;
      float[] moonOrbitRadii, moonSizes, moonSpeeds;
      int[] moonTextures;

      //Planets
      CelestialBody(String name, float orbitRadius, float size, float rotationSpeed, float orbitSpeed) {
         this.name = name;
         this.orbitRadius = orbitRadius;
         this.size = size;
         this.rotationSpeed = rotationSpeed;
         this.orbitSpeed = orbitSpeed;
      }
      //Earth and Jupiter
      CelestialBody withMoons(float[] radii, float[] sizes, int[] textures, float[] speeds) {
         this.moonOrbitRadii = radii;
         this.moonSizes = sizes;
         this.moonTextures = textures;
         this.moonSpeeds = speeds;
         return this;
      }
      //Textbox with planets 
      CelestialBody withDescription(String desc) {
         this.description = desc;
         return this;
      }
   }
   
   //-------------------------------------------------------------------------------
   
   //Constructor 
   public Code() {
      setTitle("Solar System");
      setSize(1000, 1000);
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      setupUI(); // For labels
      
      //Animation 
      startTime = stageStartTime = System.currentTimeMillis();
      animator = new FPSAnimator(myPanel, 60, true);
      animator.start();
      updateInfoLabel();
   }
   
   //-------------------------------------------------------------------------------
   
   //Builds and sets up labels
   private void setupUI() {
      JLayeredPane layeredPane = new JLayeredPane();
      layeredPane.setLayout(null);
      
      //OpenGL rendering panel
      myPanel = new GLJPanel();
      myPanel.setBounds(0, 0, 1000, 1000);
      myPanel.addGLEventListener(this);
      myPanel.addKeyListener(this);
      myPanel.setFocusable(true);
      myPanel.addMouseListener(new MouseAdapter() {
         public void mouseClicked(MouseEvent e) { myPanel.requestFocus(); }
      });
      
      //Control info label
      infoLabel = new JLabel();
      infoLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
      infoLabel.setForeground(Color.WHITE);
      infoLabel.setVerticalAlignment(SwingConstants.TOP);
      infoLabel.setBounds(10, 10, 220, 400);
      
      //Planet description label 
      planetInfoLabel = new JLabel();
      planetInfoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
      planetInfoLabel.setForeground(Color.WHITE);
      planetInfoLabel.setVerticalAlignment(SwingConstants.TOP);
      planetInfoLabel.setBounds(40, 800, 600, 100);
      planetInfoLabel.setVisible(false);
      
      //UI layers
      layeredPane.add(myPanel, JLayeredPane.DEFAULT_LAYER);
      layeredPane.add(infoLabel, JLayeredPane.PALETTE_LAYER);
      layeredPane.add(planetInfoLabel, JLayeredPane.PALETTE_LAYER);
      
      //Window resize
      this.addComponentListener(new ComponentAdapter() {
         public void componentResized(ComponentEvent e) {
            Dimension size = layeredPane.getSize();
            myPanel.setBounds(0, 0, size.width, size.height);
         }
      });
      
      this.add(layeredPane);
      this.setVisible(true);
      myPanel.requestFocus();
   }

//-------------------------------------------------------------------------------

   //Initalizes all planets and their properties
   private void initializePlanets() {
      planets = new CelestialBody[] {
         new CelestialBody("Mercury", 58.0f, 0.38f, 0.3f, 0.4f)
            .withDescription("The smallest planet and closest to the Sun, Mercury experiences extreme<br>temperature swings because it has almost no atmosphere."),
         
         new CelestialBody("Venus", 108.0f, 0.95f, 0.5f, 0.3f)
            .withDescription("Often called Earth's sister planet due to its size, Venus is covered in<br>thick carbon-dioxide clouds that trap heat, making it the hottest planet."),
         
         new CelestialBody("Earth", 150.0f, 1.0f, 0.5f, 0.2f)
            .withDescription("The only known world with life, Earth has liquid water, a protective<br>atmosphere, and a magnetic field that shields it from solar radiation.")
            .withMoons(new float[]{4.0f}, new float[]{0.27f}, new int[]{0}, new float[]{0.5f}),
         
         new CelestialBody("Mars", 228.0f, 0.53f, 0.4f, 0.1f)
            .withDescription("Mars hosts the tallest volcano (Olympus Mons) and one of the longest<br>canyons (Valles Marineris) in the Solar System."),
         
         new CelestialBody("Jupiter", 778.0f, 9.0f, 1.0f, 0.05f)
            .withDescription("The largest planet, Jupiter is a gas giant with a massive magnetic field<br>and dozens of moons. Its Great Red Spot is a storm larger than Earth.")
            .withMoons(new float[]{15.0f, 20.0f, 26.0f, 33.0f}, new float[]{0.29f, 0.24f, 0.41f, 0.38f}, 
                       new int[]{0, 0, 0, 0}, new float[]{0.3f, 0.25f, 0.15f, 0.1f}),
         
         new CelestialBody("Saturn", 1430.0f, 9.4f, 0.9f, 0.03f)
            .withDescription("Known for its spectacular ring system, Saturn is another gas giant with<br>a very low density—low enough to float in water theoretically."),
         
         new CelestialBody("Uranus", 2870.0f, 4.0f, 0.8f, 0.02f)
            .withDescription("An ice giant with a pale blue-green color, Uranus rotates on its side<br>due to an extreme axial tilt, causing unusual 20-year seasons."),
         
         new CelestialBody("Neptune", 4500.0f, 3.9f, 0.6f, 0.01f)
            .withDescription("Neptune is a deep-blue ice giant with the fastest winds recorded in the<br>Solar System. Its moon Triton orbits backward."),
         
         new CelestialBody("Pluto", 5900.0f, 0.18f, 0.5f, 0.008f)
            .withDescription("A dwarf planet in the Kuiper Belt, Pluto has a surprisingly complex surface<br>with nitrogen-ice plains and mountains of water ice.")
      };
   }

//-------------------------------------------------------------------------------

   //Main Display 
   public void display(GLAutoDrawable drawable) {
      GL4 gl = (GL4) GLContext.getCurrentGL();
      
      //Calculate time 
      float gt = timeStopped ? frozenTime : (System.currentTimeMillis() - startTime) / 1000.0f;
      
      if (topDownMode) { //In top-down mode
         gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
         gl.glUseProgram(renderingProgram2);
         displayTopDown(gl, gt);
      } else {
         //In navigation mode
         float elapsed = (System.currentTimeMillis() - stageStartTime) / 1000.0f;
         Vector3f camPos = new Vector3f();
         Vector3f lookAt = new Vector3f();
         calculateCameraPosition(gt, elapsed, camPos, lookAt);
         currentCamPos.set(camPos);
         
         passOne(gl, gt);   //Shadow map
         passTwo(gl, gt);  //Render scene
      }
   }

//-------------------------------------------------------------------------------
   
   //Render top-down view
   private void displayTopDown(GL4 gl, float gt) {
      gl.glUniform1i(useLightingLoc, 0);  
      
      //Projection matrix
      if (projectionNeedsUpdate) {
         aspect = (float) myPanel.getWidth() / myPanel.getHeight();
         float viewSize = scaledViewMode ? 100.0f : 350.0f;
         pMat.identity().setOrtho(-viewSize * aspect, viewSize * aspect, -viewSize, viewSize, 0.1f, 8000.0f);
         gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
         projectionNeedsUpdate = false;
      }
      
      //Camera 
      vMat.identity().lookAt(0.0f, 500.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f);
      mvStack.pushMatrix();
      mvStack.mul(vMat);
      setupVertexAttributes(gl);
      
      float orbitScale = scaledViewMode ? 1.0f : 0.055f;
      
      //Orbit lines
      gl.glUniform1i(useTextureLoc, 0);
      gl.glUniform4f(colorLoc, 1.0f, 1.0f, 1.0f, 1.0f);
      gl.glLineWidth(1.0f);
      
      float[] scaledOrbitRadii = {6.0f, 9.0f, 12.0f, 15.0f, 22.0f, 30.0f, 40.0f, 50.0f, 59.0f};
      
      mvStack.pushMatrix();
      for (int i = 0; i < Math.min(planets.length, 9); i++) {
         mvStack.pushMatrix();
         float scale = scaledViewMode ? scaledOrbitRadii[i] / planets[i].orbitRadius : orbitScale;
         mvStack.scale(scale, scale, scale);
         gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5 + i]);
         gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
         gl.glDisableVertexAttribArray(1);
         gl.glUniformMatrix4fv(mvLoc, 1, false, mvStack.get(vals));
         gl.glDrawArrays(GL_LINE_LOOP, 0, numOrbitVerts);
         mvStack.popMatrix();
      }
      mvStack.popMatrix();
      
      setupVertexAttributes(gl);
      gl.glUniform1i(useTextureLoc, 1);
      
      //Sun
      float sunSize = scaledViewMode ? 3.0f : 1.2f;
      renderSphere(gl, new Vector3f(0, 0, 0), sunSize, gt * 0.2f, sunTexture);
      
      //Asteroid belt
      float asteroidPos = scaledViewMode ? 21.0f : 420.0f * orbitScale;
      float asteroidThickness = scaledViewMode ? 3.0f : 15.0f;
      renderTexturedBelt(gl, asteroidPos, asteroidThickness, asteroidTexture, gt * 0.05f);      
      
      float[] planetSizes = scaledViewMode ? 
         new float[]{1.0f, 1.5f, 1.6f, 1.2f, 4.0f, 3.5f, 2.5f, 2.5f, 0.8f} :
         new float[]{0.25f, 0.5f, 0.55f, 0.35f, 3.5f, 3.2f, 1.8f, 1.7f, 0.15f};
      
      float moonSize = scaledViewMode ? 0.25f : 0.15f;
      float moonOrbitScale = 0.4f;
      
      //Planets
      for (int i = 0; i < planets.length; i++) {
         CelestialBody planet = planets[i];
         float px, pz;
         
         if (scaledViewMode) {
            px = (float)Math.cos(gt * planet.orbitSpeed) * scaledOrbitRadii[i];
            pz = (float)Math.sin(gt * planet.orbitSpeed) * scaledOrbitRadii[i];
         } else {
            Vector3f pos = getPlanetPosition(i, gt);
            px = pos.x * orbitScale;
            pz = pos.z * orbitScale;
         }
         
         renderSphere(gl, new Vector3f(px, 0, pz), planetSizes[i], gt * planet.rotationSpeed, planet.texture);
         
         //Moons
         if (planet.moonTextures != null) {
            float[] moonOrbits = (i == 4 && scaledViewMode) ? 
               new float[]{2.0f, 2.8f, 3.6f, 4.5f} : planet.moonOrbitRadii;
            
            for (int m = 0; m < planet.moonTextures.length; m++) {
               float moonAngle = gt * planet.moonSpeeds[m];
               float moonX = px + (float)Math.cos(moonAngle) * moonOrbits[m] * moonOrbitScale;
               float moonZ = pz + (float)Math.sin(moonAngle) * moonOrbits[m] * moonOrbitScale;
               renderSphere(gl, new Vector3f(moonX, 0, moonZ), moonSize, gt * 0.1f, planet.moonTextures[m]);
            }
         }
      }
      
      //Kuiper belt
      float kuiperPos = scaledViewMode ? 65.0f : 6000.0f * orbitScale;
      float kuiperThickness = scaledViewMode ? 5.0f : 20.0f;
      renderTexturedBelt(gl, kuiperPos, kuiperThickness, asteroidTexture, gt * 0.02f);      
      
      mvStack.popMatrix();
   }

   //-----------------------------------------------------------------------------------------------
   
   //Shadow pass: Renders scene from camera's perspective to create shadow map.
   private void passOne(GL4 gl, float gt) {
      boolean hasMoons = (currentStage == 2 || currentStage == 4);
      
      //Clear shadow buffer
      gl.glBindFramebuffer(GL_FRAMEBUFFER, shadowBuffer);
      gl.glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, shadowTex, 0);
      gl.glDrawBuffer(GL_NONE);
      gl.glClearDepth(1.0);
      gl.glClear(GL_DEPTH_BUFFER_BIT);
      
      if (!hasMoons || isTransitioning || returningToSun) {
         gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
         return;
      }
      
      gl.glUseProgram(renderingProgram1);
      
      CelestialBody planet = planets[currentStage];
      Vector3f targetPos = new Vector3f(lockedPlanetPos);
      
      //Calculate orthographic projection size 
      float maxMoonOrbit = 0;
      for (float orbit : planet.moonOrbitRadii) {
         maxMoonOrbit = Math.max(maxMoonOrbit, orbit);
      }
      float orthoSize = Math.max(planet.size * 5, maxMoonOrbit * 2.0f);
      
      //For  shadow precision
      float distToTarget = currentCamPos.distance(targetPos);
      float systemRadius = Math.max(planet.size * 3, maxMoonOrbit + planet.size);
      float nearPlane = Math.max(0.1f, distToTarget - systemRadius - 10.0f);
      float farPlane = distToTarget + systemRadius + 10.0f;
      
      lightVmat.identity().lookAt(currentCamPos.x, currentCamPos.y, currentCamPos.z,targetPos.x, targetPos.y, targetPos.z, 0.0f, 1.0f, 0.0f);
      lightPmat.identity().setOrtho(-orthoSize, orthoSize, -orthoSize, orthoSize, nearPlane, farPlane);
      
      gl.glEnable(GL_DEPTH_TEST);
      gl.glEnable(GL_POLYGON_OFFSET_FILL);
      gl.glPolygonOffset(2.0f, 4.0f);
      
      int shadowMVPLoc1 = gl.glGetUniformLocation(renderingProgram1, "shadowMVP");
      mvStack.pushMatrix();
      mvStack.mul(lightVmat);
      setupVertexAttributes(gl);
      
      //Render moons for shadows
      for (int m = 0; m < planet.moonTextures.length; m++) {
         float moonAngle = gt * planet.moonSpeeds[m];
         float moonX = lockedPlanetPos.x + (float)Math.cos(moonAngle) * planet.moonOrbitRadii[m];
         float moonZ = lockedPlanetPos.z + (float)Math.sin(moonAngle) * planet.moonOrbitRadii[m];
         Vector3f moonPos = new Vector3f(moonX, lockedPlanetPos.y, moonZ);
         
         if (currentCamPos.distance(moonPos) < currentCamPos.distance(targetPos)) {
            renderSpherePass1(gl, moonPos, planet.moonSizes[m], shadowMVPLoc1);
         }
      }
      
      mvStack.popMatrix();
      gl.glDisable(GL_POLYGON_OFFSET_FILL);
      gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
   }

   //---------------------------------------------------------------------
   
   //Main Pass: Render scene with shadow map
   private void passTwo(GL4 gl, float gt) {
      gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
      gl.glUseProgram(renderingProgram2);
      
      //Update projection 
      if (projectionNeedsUpdate) {
         aspect = (float) myPanel.getWidth() / myPanel.getHeight();
         pMat.identity().setPerspective((float)Math.toRadians(60.0f), aspect, 0.1f, 8000.0f);
         projectionNeedsUpdate = false;
      }
      
      //Bind shadow map 
      gl.glActiveTexture(GL_TEXTURE1);
      gl.glBindTexture(GL_TEXTURE_2D, shadowTex);
      gl.glUniform1i(gl.glGetUniformLocation(renderingProgram2, "shadowTex"), 1);
      
      //Calculate camera position
      float elapsed = (System.currentTimeMillis() - stageStartTime) / 1000.0f;
      Vector3f camPos = new Vector3f();
      Vector3f lookAt = new Vector3f();
      calculateCameraPosition(gt, elapsed, camPos, lookAt);
      currentCamPos.set(camPos);
      
      vMat.identity().lookAt(camPos.x, camPos.y, camPos.z, lookAt.x, lookAt.y, lookAt.z, 0, 1, 0);
      
      mvStack.pushMatrix();
      mvStack.mul(vMat);
      setupVertexAttributes(gl);
      
      renderSkydome(gl, camPos);
      
      //Lighting 
      gl.glUniform1i(useLightingLoc, 1);
      gl.glUniform3f(lightPosLoc, camPos.x, camPos.y, camPos.z);
      gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
      
      //Sun 
      gl.glUniform1i(useLightingLoc, 0);
      gl.glUniform1i(useShadowsLoc, 0);
      renderSphere(gl, new Vector3f(0, 0, 0), 2.5f, gt * 1.5f, sunTexture);
      gl.glUniform1i(useLightingLoc, 1);
      
      //Planets and moons
      for (int i = 0; i < planets.length; i++) {
         CelestialBody planet = planets[i];
         Vector3f pos = (!isTransitioning && currentStage == i) ? lockedPlanetPos : getPlanetPosition(i, gt);
         
         //Only Earth and Jupiter have shadows
         boolean receiveShadows = !isTransitioning && (currentStage == 2 || currentStage == 4) && currentStage == i;
         gl.glUniform1i(useShadowsLoc, receiveShadows ? 1 : 0);
         
         renderSphere(gl, pos, planet.size, gt * planet.rotationSpeed, planet.texture);
         
         //Saturn's rings
         if (i == 5) {
            gl.glUniform1i(useShadowsLoc, 0);
            renderSaturnRings(gl, pos, planet.size, gt);
         }
         
         //Moons
         if (planet.moonTextures != null) {
            gl.glUniform1i(useShadowsLoc, 0);
            for (int m = 0; m < planet.moonTextures.length; m++) {
               float moonAngle = gt * planet.moonSpeeds[m];
               Vector3f moonPos = new Vector3f(
                  pos.x + (float)Math.cos(moonAngle) * planet.moonOrbitRadii[m],
                  pos.y,
                  pos.z + (float)Math.sin(moonAngle) * planet.moonOrbitRadii[m]
               );
               renderSphere(gl, moonPos, planet.moonSizes[m], gt * 0.5f, planet.moonTextures[m]);
            }
         }
      }
      
      //Update planet info display
      if (!isTransitioning) {
         if (currentStage == -1) { //At Sun
            updatePlanetInfo("Sun", "A G-type main-sequence star containing 99% of the solar system's mass.<br>Energy from nuclear fusion produces light, heat, sunspots, and solar flares.");
         } else { //Viewing Planet
            updatePlanetInfo(planets[currentStage].name, planets[currentStage].description);
         }
      } else { //Traveling
         planetInfoLabel.setVisible(false);
      }
      
      mvStack.popMatrix();
   }

   //----------------------------------------------------------------------
   
   //Updates the control info label based on current mode
   private void updateInfoLabel() {
      StringBuilder html = new StringBuilder("<html>");
      
      if (topDownMode) {
         html.append("<b>Mode: Top-Down (").append(scaledViewMode ? "Scaled" : "Realistic").append(")</b><br><br>");
         html.append("<b>Time: ").append(timeStopped ? "Off" : "On").append("</b><br><br>");
         html.append("<b>Controls:</b><br>[T] Toggle Mode<br>[V] Switch View<br>[R] Play/Pause<br><br>");
      } else {
         String body = currentStage == -1 ? "Sun" : 
                      (currentStage < planets.length ? planets[currentStage].name : "Returning");
         html.append("<b>Mode: Navigation - ").append(body).append("</b><br><br>");
         html.append("<b>Time: ").append(timeStopped ? "Off" : "On").append("</b><br><br>");
         html.append("<b>Controls:</b><br>[T] Toggle Mode<br>[->] Next/Skip<br>[R] Play/Pause<br><br>");
         html.append("<b>Jump to Body:</b><br>");
         for (int i = 0; i <= 9; i++) {
            html.append("[").append(i).append("] ").append(i == 0 ? "Sun" : planets[i-1].name).append("<br>");
         }
      }
      
      html.append("</html>");
      infoLabel.setText(html.toString());
   }
   
   //Updates the planet info label
   private void updatePlanetInfo(String name, String description) {
      planetInfoLabel.setText("<html><b>" + name + "</b><br>" + description + "</html>");
      planetInfoLabel.setVisible(true);
   }
   
   //---------------------------------------------------------------------------------------------
   
   //Camera postioning for navagation mode
   private void calculateCameraPosition(float gt, float elapsed, Vector3f camPos, Vector3f lookAt) {
      if (!isTransitioning) {
         //Viewing Planet
         if (currentStage == -1) {
            camPos.set(0, 15, 35);
            lookAt.set(0, 0, 0);
         } else {
            CelestialBody planet = planets[currentStage];
            float distance = (currentStage == 4) ? 40.0f : (currentStage == 5) ? 38.0f : (8.0f + planet.size * 2.0f);
            camPos.set(lockedPlanetPos.x, lockedPlanetPos.y + planet.size * 0.8f, lockedPlanetPos.z + distance);
            lookAt.set(lockedPlanetPos);
         }
      } else if (returningToSun) { //Pluto --> Sun
         float t = Math.min(1.0f, elapsed / 6.0f);
         t = smoothstep(t);          
         Vector3f sunPos = new Vector3f(0, 0, 0);
         lookAt.set(new Vector3f(lockedPlanetPos).lerp(sunPos, t));
         
         //Camera follows behind, looking at target
         Vector3f direction = new Vector3f(sunPos).sub(lockedPlanetPos).normalize();
         float dist = 35.0f;
         camPos.set(new Vector3f(lookAt).sub(new Vector3f(direction).mul(dist)));
         camPos.y = 15.0f;  
         
         if (elapsed >= 6.0f) {
            isTransitioning = false;
            currentStage = -1;
            returningToSun = false;
            stageStartTime = System.currentTimeMillis();
         }
      } else { //Traveling
         float t = Math.min(1.0f, elapsed / 6.0f);
         t = smoothstep(t);
         
         //Start position
         Vector3f startCamPos;
         if (currentStage == 0) {
            startCamPos = new Vector3f(0, 15, 35);
         } else {
            CelestialBody prevPlanet = planets[currentStage - 1];
            Vector3f prevPos = getPlanetPosition(currentStage - 1, gt);
            float dist = (currentStage == 5 || currentStage == 1) ? 40.0f : (8.0f + prevPlanet.size * 2.0f);
            startCamPos = new Vector3f(prevPos.x, prevPos.y + prevPlanet.size * 0.8f, prevPos.z + dist);
         }
         
         //End position
         CelestialBody planet = planets[currentStage];
         float dist = (currentStage == 4) ? 40.0f : (currentStage == 5) ? 38.0f : (8.0f + planet.size * 2.0f);
         Vector3f endCamPos = new Vector3f(lockedPlanetPos.x, lockedPlanetPos.y + planet.size * 0.8f, lockedPlanetPos.z + dist);
         
         lookAt.set(lockedPlanetPos);
         camPos.set(new Vector3f(startCamPos).lerp(endCamPos, t));
         
         if (elapsed >= 6.0f) {
            isTransitioning = false;
            stageStartTime = System.currentTimeMillis();
         }
      }
   }
   
   //Function for smooth transitions
   private float smoothstep(float t) {
      return t * t * t * (t * (t * 6.0f - 15.0f) + 10.0f);
   }
   
   //------------------------------------------------------------------------
   
   //Traveling between planets
   private void handleNavigation(float gt) {
      if (isTransitioning) { //Skip current transition
         isTransitioning = false;
         if (returningToSun) {
            currentStage = -1;
            returningToSun = false;
         }
         stageStartTime = System.currentTimeMillis();
      } else { //Start next transition
         if (currentStage == -1) {
            currentStage = 0;
            lockedPlanetPos.set(getPlanetPosition(0, gt));
         } else if (currentStage < planets.length - 1) {
            currentStage++;
            lockedPlanetPos.set(getPlanetPosition(currentStage, gt));
         } else {
            returningToSun = true;
         }
         isTransitioning = true;
         stageStartTime = System.currentTimeMillis();
         planetInfoLabel.setVisible(false);
      }
   }
   
   //--------------------------------------------------------------------------
   
   //Jump to a planet
   private void jumpToBody(int targetIndex, float gt) {
      if (targetIndex == currentStage && !isTransitioning) return;
      
      currentStage = targetIndex;
      isTransitioning = false;
      returningToSun = false;
      stageStartTime = System.currentTimeMillis();
      
      if (currentStage >= 0 && currentStage < planets.length) {
         lockedPlanetPos.set(getPlanetPosition(currentStage, gt));
      }
   }

   //------------------------------------------------------------------------------------------
   //Calculates planet position time
   private Vector3f getPlanetPosition(int index, float gt) {
      CelestialBody planet = planets[index];
      float angle = gt * planet.orbitSpeed;
      return new Vector3f(
         planet.orbitRadius * (float)Math.cos(angle),
         0.0f,
         planet.orbitRadius * (float)Math.sin(angle)
      );
   }
   
   //---------------------------------------------------------------------------------------------------
   
   //Initalization 
   public void init(GLAutoDrawable drawable) {
      GL4 gl = (GL4) GLContext.getCurrentGL();
      gl.setSwapInterval(1);  //Enable VSync
      
      //Shaders
      renderingProgram1 = Utils.createShaderProgram("code/vertShader1.glsl", "code/fragShader1.glsl");
      renderingProgram2 = Utils.createShaderProgram("code/vertShader2.glsl", "code/fragShader2.glsl");
      
      //Compiles and Link Shaders
      gl.glUseProgram(renderingProgram2);
      mvLoc = gl.glGetUniformLocation(renderingProgram2, "mv_matrix");
      pLoc = gl.glGetUniformLocation(renderingProgram2, "p_matrix");
      normLoc = gl.glGetUniformLocation(renderingProgram2, "norm_matrix");
      colorLoc = gl.glGetUniformLocation(renderingProgram2, "color");
      useTextureLoc = gl.glGetUniformLocation(renderingProgram2, "useTexture");
      shadowMVPLoc = gl.glGetUniformLocation(renderingProgram2, "shadowMVP");
      lightPosLoc = gl.glGetUniformLocation(renderingProgram2, "lightPos");
      useLightingLoc = gl.glGetUniformLocation(renderingProgram2, "useLighting");
      useShadowsLoc = gl.glGetUniformLocation(renderingProgram2, "useShadows");
      
      int texLoc = gl.glGetUniformLocation(renderingProgram2, "samp");
      if (texLoc >= 0) gl.glUniform1i(texLoc, 0);
      
      gl.glEnable(GL_DEPTH_TEST);
      gl.glDepthFunc(GL_LEQUAL);
      gl.glEnable(GL_CULL_FACE);
      gl.glFrontFace(GL_CCW);
      
      //Projection matrix
      aspect = (float) myPanel.getWidth() / (float) myPanel.getHeight();
      pMat.identity().setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 8000.0f);
      gl.glViewport(0, 0, myPanel.getWidth(), myPanel.getHeight());
      
      startTime = System.currentTimeMillis();
      
      //Initialize scene data
      initializePlanets();
      setupVertices();
      setupOrbitLines();
      loadTextures();
      setupShadowBuffers(gl);
   }
   
   //-------------------------------------------------------------------------------
   
   //Loads Textures
   private void loadTextures() {
      sunTexture = Utils.loadTexture("sun.jpg");
      skydomeTexture = Utils.loadTexture("skydome.png");
      asteroidTexture = Utils.loadTexture("asteroidBelt.png");
      saturnRingTexture = Utils.loadTexture("saturnsRings.png");
      
      String[] textureFiles = {"mercury.jpg", "venus.jpg", "earth.jpg", "mars.jpg", 
                               "jupiter.jpg", "saturn.jpg", "uranus.jpg", "neptune.jpg", "pluto.jpg"};
      for (int i = 0; i < planets.length; i++) {
         planets[i].texture = Utils.loadTexture(textureFiles[i]);
      }
      
      // Moon textures
      planets[2].moonTextures[0] = Utils.loadTexture("moon.jpg");
      planets[4].moonTextures[0] = Utils.loadTexture("io.jpg");
      planets[4].moonTextures[1] = Utils.loadTexture("europa.jpg");
      planets[4].moonTextures[2] = Utils.loadTexture("ganymede.jpg");
      planets[4].moonTextures[3] = Utils.loadTexture("callisto.jpg");
   }
      
   //-------------------------------------------------------------------------------
   
   //Sphere Vertices
   private void setupVertices() {
      GL4 gl = (GL4) GLContext.getCurrentGL();
      mySphere = new Sphere(48);  
      numSphereVerts = mySphere.getIndices().length;
      
      int[] indices = mySphere.getIndices();
      Vector3f[] vert = mySphere.getVertices();
      Vector2f[] tex = mySphere.getTexCoords();
      Vector3f[] norm = mySphere.getNormals();
      
      float[] pvalues = new float[indices.length * 3];  // Positions
      float[] tvalues = new float[indices.length * 2];  // Texture coords
      float[] nvalues = new float[indices.length * 3];  // Normals
      
      for (int i = 0; i < indices.length; i++) {
         int idx = indices[i];
         pvalues[i*3] = vert[idx].x;
         pvalues[i*3+1] = vert[idx].y;
         pvalues[i*3+2] = vert[idx].z;
         tvalues[i*2] = tex[idx].x;
         tvalues[i*2+1] = tex[idx].y;
         nvalues[i*3] = norm[idx].x;
         nvalues[i*3+1] = norm[idx].y;
         nvalues[i*3+2] = norm[idx].z;
      }
      
      //Create and bind VAO/VBOs
      gl.glGenVertexArrays(vao.length, vao, 0);
      gl.glBindVertexArray(vao[0]);
      gl.glGenBuffers(vbo.length, vbo, 0);
      
      //Positions
      gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
      gl.glBufferData(GL_ARRAY_BUFFER, pvalues.length*4, Buffers.newDirectFloatBuffer(pvalues), GL_STATIC_DRAW);
      //Texture coords
      gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
      gl.glBufferData(GL_ARRAY_BUFFER, tvalues.length*4, Buffers.newDirectFloatBuffer(tvalues), GL_STATIC_DRAW);
      //Dynamic texture coords (rings/belts)
      gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
      gl.glBufferData(GL_ARRAY_BUFFER, 1000*4, null, GL_DYNAMIC_DRAW);
      //Normals
      gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
      gl.glBufferData(GL_ARRAY_BUFFER, nvalues.length*4, Buffers.newDirectFloatBuffer(nvalues), GL_STATIC_DRAW);
      //Dynamic positions (rings/belts)
      gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[14]);
      gl.glBufferData(GL_ARRAY_BUFFER, 10000*4, null, GL_DYNAMIC_DRAW);
   }
   
   //-------------------------------------------------------------------------------
   
   //Shadow Buffers
   private void setupShadowBuffers(GL4 gl) {
      int scSizeX = myPanel.getWidth();
      int scSizeY = myPanel.getHeight();
      
      int[] shadowBuf = new int[1];
      int[] shadowTx = new int[1];
      
      gl.glGenFramebuffers(1, shadowBuf, 0);
      shadowBuffer = shadowBuf[0];
      
      gl.glGenTextures(1, shadowTx, 0);
      shadowTex = shadowTx[0];
      
      //Shadow map texture
      gl.glBindTexture(GL_TEXTURE_2D, shadowTex);
      gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32, scSizeX, scSizeY, 0, GL_DEPTH_COMPONENT, GL_FLOAT, null);
      gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
      gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
      gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
      gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
      gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
      gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
   }
   
   //--------------------------------------------------------------------------------------
   
   //Binds vertex attributes
   private void setupVertexAttributes(GL4 gl) {
      gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
      gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
      gl.glEnableVertexAttribArray(0);
      
      gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
      gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
      gl.glEnableVertexAttribArray(1);
      
      gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
      gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
      gl.glEnableVertexAttribArray(2);
   }
   
   //-------------------------------------------------------------------------------
   
   //Method to Draw the Orbit Lines While in Top-down Mode
   private void setupOrbitLines() {
      GL4 gl = (GL4) GLContext.getCurrentGL();
      int segments = 100;  
      numOrbitVerts = segments;
      
      //Store orbit lines for planets
      for (int i = 0; i < Math.min(planets.length, 9); i++) {
         float radius = planets[i].orbitRadius;
         float[] orbitVerts = new float[segments * 3];
         
         for (int j = 0; j < segments; j++) {
            float angle = (float)(2.0 * Math.PI * j / segments);
            orbitVerts[j*3] = radius * (float)Math.cos(angle);
            orbitVerts[j*3+1] = 0.0f;
            orbitVerts[j*3+2] = radius * (float)Math.sin(angle);
         }
         
         gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5 + i]);
         gl.glBufferData(GL_ARRAY_BUFFER, orbitVerts.length*4, Buffers.newDirectFloatBuffer(orbitVerts), GL_STATIC_DRAW);
      }
   }
   
   //-------------------------------------------------------------------------------  
  
   //Render shadow sphere
   private void renderSpherePass1(GL4 gl, Vector3f pos, float scale, int mvpLoc) {
      mvStack.pushMatrix();
      mvStack.translate(pos.x, pos.y, pos.z);
      mvStack.scale(scale, scale, scale);
      Matrix4f shadowMVP = new Matrix4f(lightPmat).mul(mvStack);
      gl.glUniformMatrix4fv(mvpLoc, 1, false, shadowMVP.get(vals));
      gl.glDrawArrays(GL_TRIANGLES, 0, numSphereVerts);
      mvStack.popMatrix();
   }
   
    //-------------------------------------------------------------------------------  
    
   //Renders the planet, moons, sun
   private void renderSphere(GL4 gl, Vector3f pos, float scale, float rotation, int texture) {
      gl.glActiveTexture(GL_TEXTURE0);
      gl.glBindTexture(GL_TEXTURE_2D, texture);
      gl.glUniform1i(useTextureLoc, 1);
      
      mvStack.pushMatrix();
      mvStack.translate(pos.x, pos.y, pos.z);
      mvStack.pushMatrix();
      mvStack.rotate(rotation % (float)(2.0 * Math.PI), 0.0f, 1.0f, 0.0f);
      mvStack.scale(scale, scale, scale);
      
      //Normal matrix for lighting 
      Matrix4f invTrMat = new Matrix4f(mvStack).invert().transpose();
      gl.glUniformMatrix4fv(normLoc, 1, false, invTrMat.get(vals));
      gl.glUniformMatrix4fv(mvLoc, 1, false, mvStack.get(vals));
      
      //Shadow MVP matrix
      Matrix4f modelMatrix = new Matrix4f().translate(pos.x, pos.y, pos.z)
                                           .rotate(rotation % (float)(2.0 * Math.PI), 0.0f, 1.0f, 0.0f)
                                           .scale(scale, scale, scale);
      Matrix4f shadowMVP = new Matrix4f(lightPmat).mul(lightVmat).mul(modelMatrix);
      gl.glUniformMatrix4fv(shadowMVPLoc, 1, false, shadowMVP.get(vals));
      
      gl.glDrawArrays(GL_TRIANGLES, 0, numSphereVerts);
      mvStack.popMatrix();
      mvStack.popMatrix();
   }
   
   //-------------------------------------------------------------------------------
   
   //Renders Saturn's rings.
   private void renderSaturnRings(GL4 gl, Vector3f pos, float planetSize, float gt) {
      gl.glActiveTexture(GL_TEXTURE0);
      gl.glBindTexture(GL_TEXTURE_2D, saturnRingTexture);
      gl.glUniform1i(useTextureLoc, 1);
      gl.glDisable(GL_CULL_FACE);
      gl.glEnable(GL_BLEND);
      gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
      
      float innerX = planetSize * 1.4f, outerX = planetSize * 2.3f;
      float innerZ = planetSize * 1.7f, outerZ = planetSize * 2.8f;
      float tilt = 12.0f;
      int segments = 120;
      
      mvStack.pushMatrix();
      mvStack.translate(pos.x, pos.y, pos.z);
      mvStack.rotate((float)Math.toRadians(tilt), 1.0f, 0.0f, 0.0f);
      mvStack.rotate(gt * 0.1f, 0.0f, 1.0f, 0.0f);
      
      float[] ringVerts = new float[segments * 18];
      float[] ringTexCoords = new float[segments * 12];
      
      for (int i = 0; i < segments; i++) {
         float a1 = (float)(2.0 * Math.PI * i / segments);
         float a2 = (float)(2.0 * Math.PI * (i + 1) / segments);
         float c1 = (float)Math.cos(a1), s1 = (float)Math.sin(a1);
         float c2 = (float)Math.cos(a2), s2 = (float)Math.sin(a2);
         float u1 = (float)i / segments, u2 = (float)(i + 1) / segments;
         
         //Two triangles per segment
         int vi = i * 18, ti = i * 12;
         ringVerts[vi] = innerX*c1; ringVerts[vi+1] = 0; ringVerts[vi+2] = innerZ*s1;
         ringVerts[vi+3] = outerX*c1; ringVerts[vi+4] = 0; ringVerts[vi+5] = outerZ*s1;
         ringVerts[vi+6] = innerX*c2; ringVerts[vi+7] = 0; ringVerts[vi+8] = innerZ*s2;
         ringVerts[vi+9] = outerX*c1; ringVerts[vi+10] = 0; ringVerts[vi+11] = outerZ*s1;
         ringVerts[vi+12] = outerX*c2; ringVerts[vi+13] = 0; ringVerts[vi+14] = outerZ*s2;
         ringVerts[vi+15] = innerX*c2; ringVerts[vi+16] = 0; ringVerts[vi+17] = innerZ*s2;
         
         ringTexCoords[ti] = u1; ringTexCoords[ti+1] = 0;
         ringTexCoords[ti+2] = u1; ringTexCoords[ti+3] = 1;
         ringTexCoords[ti+4] = u2; ringTexCoords[ti+5] = 0;
         ringTexCoords[ti+6] = u1; ringTexCoords[ti+7] = 1;
         ringTexCoords[ti+8] = u2; ringTexCoords[ti+9] = 1;
         ringTexCoords[ti+10] = u2; ringTexCoords[ti+11] = 0;
      }
      
      gl.glUniformMatrix4fv(mvLoc, 1, false, mvStack.get(vals));
      
      gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[14]);
      gl.glBufferData(GL_ARRAY_BUFFER, ringVerts.length*4, Buffers.newDirectFloatBuffer(ringVerts), GL_DYNAMIC_DRAW);
      gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
      gl.glEnableVertexAttribArray(0);
      
      gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
      gl.glBufferData(GL_ARRAY_BUFFER, ringTexCoords.length*4, Buffers.newDirectFloatBuffer(ringTexCoords), GL_DYNAMIC_DRAW);
      gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
      gl.glEnableVertexAttribArray(1);
      
      gl.glDrawArrays(GL_TRIANGLES, 0, segments * 6);
      
      mvStack.popMatrix();
      gl.glDisable(GL_BLEND);
      gl.glEnable(GL_CULL_FACE);
      setupVertexAttributes(gl);
   }
   
   //------------------------------------------------------------------------------
   
   //Renders asteroid and Kuiper belt     
   private void renderTexturedBelt(GL4 gl, float position, float size, int texture, float rotation) {
      gl.glActiveTexture(GL_TEXTURE0);
      gl.glBindTexture(GL_TEXTURE_2D, texture);
      gl.glUniform1i(useTextureLoc, 1);
      gl.glDisable(GL_CULL_FACE);
      gl.glEnable(GL_BLEND);
      gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
      gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
      gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    
      float inner = position - size;  //Inner edge distance from center
      float outer = position + size;  //Outer edge distance from center
    
      //Ring geometry using two quads 
      float[] quadVerts = {
         -outer, 0, -outer,
         -outer, 0,  outer,
          outer, 0,  outer,
         
          outer, 0,  outer,
          outer, 0, -outer,
         -outer, 0, -outer
      };
    
      //Texture coordinates 
      float[] quadTex = {
         0, 0,
         0, 1,
         1, 1,
        
         1, 1,
         1, 0,
         0, 0
      };
    
      mvStack.pushMatrix();
      mvStack.rotate(rotation, 0.0f, 1.0f, 0.0f);
      gl.glUniformMatrix4fv(mvLoc, 1, false, mvStack.get(vals));
    
      //Vertex 
      gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[14]);
      gl.glBufferData(GL_ARRAY_BUFFER, quadVerts.length * 4, Buffers.newDirectFloatBuffer(quadVerts), GL_DYNAMIC_DRAW);
      gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
      gl.glEnableVertexAttribArray(0);
    
      //Texture coordinate 
      gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
      gl.glBufferData(GL_ARRAY_BUFFER, quadTex.length * 4, Buffers.newDirectFloatBuffer(quadTex), GL_DYNAMIC_DRAW);
      gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
      gl.glEnableVertexAttribArray(1);
    
      gl.glDrawArrays(GL_TRIANGLES, 0, 6);
    
      mvStack.popMatrix();
      gl.glDisable(GL_BLEND);
      gl.glEnable(GL_CULL_FACE);
     setupVertexAttributes(gl);
   }
   
   //-----------------------------------------------------------------------------
      
   //Render Skydome
   private void renderSkydome(GL4 gl, Vector3f camPos) {
      gl.glDepthFunc(GL_LEQUAL);
      gl.glDepthMask(false);
      gl.glDisable(GL_CULL_FACE);
      gl.glUniform1i(useTextureLoc, 1);
      gl.glUniform1i(useLightingLoc, 0);
      
      mvStack.pushMatrix();
      mvStack.translate(camPos.x, camPos.y, camPos.z);
      mvStack.scale(7500.0f, 7500.0f, 7500.0f);
      
      gl.glActiveTexture(GL_TEXTURE0);
      gl.glBindTexture(GL_TEXTURE_2D, skydomeTexture);
      gl.glUniform4f(colorLoc, 1.0f, 1.0f, 1.0f, 1.0f);
      gl.glUniformMatrix4fv(mvLoc, 1, false, mvStack.get(vals));
      gl.glDrawArrays(GL_TRIANGLES, 0, numSphereVerts);
      
      mvStack.popMatrix();
      gl.glDepthMask(true);
      gl.glDepthFunc(GL_LESS);
      gl.glEnable(GL_CULL_FACE);
   }
  
   //-----------------------------------------------------------------------
   
   //Controls
   public void keyPressed(KeyEvent e) {
      float gt = timeStopped ? frozenTime : (System.currentTimeMillis() - startTime) / 1000.0f;
      
      switch (e.getKeyCode()) {
         case KeyEvent.VK_T:  //[T] --> Toggle between top-down and navigation mode
            topDownMode = !topDownMode;
            projectionNeedsUpdate = true;
            if (!topDownMode) {
               currentStage = -1;
               isTransitioning = false;
               returningToSun = false;
               stageStartTime = System.currentTimeMillis();
            } else {
               planetInfoLabel.setVisible(false);
            }
            break;
            
         case KeyEvent.VK_V:  //[V] --> Toggle scaled/realistic view (top-down only)
            if (topDownMode) {
               scaledViewMode = !scaledViewMode;
               projectionNeedsUpdate = true;
            }
            break;
            
         case KeyEvent.VK_R:  //[R] --> Play/Pause time
            if (timeStopped) {
               startTime = System.currentTimeMillis() - (long)(frozenTime * 1000.0f);
               timeStopped = false;
            } else {
               frozenTime = (System.currentTimeMillis() - startTime) / 1000.0f;
               timeStopped = true;
            }
            break;
            
         case KeyEvent.VK_RIGHT:  //[>>] --> Navigate to next planet
            if (!topDownMode) handleNavigation(gt);
            break;
            
         default:  //Number keys 0-9 jump to specific planets
            if (!topDownMode && e.getKeyCode() >= KeyEvent.VK_0 && e.getKeyCode() <= KeyEvent.VK_9) {
               jumpToBody(e.getKeyCode() - KeyEvent.VK_0 - 1, gt);
            }
            break;
      }
      
      updateInfoLabel();
   }
   
   //-------------------------------------------------------------------------------------
   
   //For switching between top-down and navagation mode
   public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
      GL4 gl = (GL4) GLContext.getCurrentGL();
      gl.glViewport(0, 0, width, height);
      projectionNeedsUpdate = true;
   }
   
   //-------------------------------------------------------------------------------------
   
   //Necessary Methods
   public void keyReleased(KeyEvent e) {}
   public void keyTyped(KeyEvent e) {}
   public void dispose(GLAutoDrawable drawable) {}
   
   public static void main(String[] args) { new Code(); }
}