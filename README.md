# Solar System Visualization

A real-time 3D solar system simulation built with Java and OpenGL (JOGL), featuring custom GLSL shaders, shadow mapping, procedural geometry, and two interactive camera modes. Developed as an honors project for CS 465 (Computer Graphics) at Western Illinois University.

![Top Down](https://github.com/user-attachments/assets/683f06f3-b702-459b-b067-27b9e5ccf5da)
---

## Overview 
This project implements a fully interactive solar system from scratch using core OpenGL concepts — no game engine, no high-level framework. Every celestial body, shader, and rendering pass was built and tuned by hand, giving me deep hands-on experience with the graphics pipeline, 3D mathematics, and real-time rendering techniques.

---

## Features

#### Dual View Modes
- **Navigation Mode** — Fly through the solar system and visit each planet up close with smooth interpolated camera transitions
- **Top-Down Mode** — View the entire solar system from above, with toggleable realistic or scaled orbital distances

#### Rendering & Graphics
- Real-time shadow mapping using a two-pass rendering pipeline (Earth and Jupiter's moons)
- Custom GLSL vertex and fragment shaders for lighting and shadow calculations
- High-resolution texture mapping on all planets, moons, and rings
- Saturn's ring system, asteroid belt, and Kuiper belt
- High-resolution space skybox

#### Orbital Mechanics
- Realistic orbital speeds relative to actual planetary periods
- Continuous axial rotation for all planets and moons
- Toggleable scaling between realistic and visualization-friendly distances

#### Interactive Controls
- Smooth camera transitions with smoothstep interpolation
- Time control (pause/resume orbital motion)
- Direct navigation to any celestial body

---

## Technical Highlights 

#### Rendering Pipeline
- **Shadow Pass** — Scene rendered from the light's perspective to generate a depth map
- **Main Pass** — Full scene rendered with lighting, textures, and shadow application using the depth map

#### Graphics Techniques
- Two-pass shadow mapping for realistic moon shadows
- Procedural sphere geometry generation (Sphere.java)
- JOML for matrix transformations (model, view, projection)
- Smoothstep interpolation for camera transitions

---

## Technologies

- **Java** (JDK 11+)
- **JOGL** (Java OpenGL) — rendering and shader pipeline
- **JOML** — matrix and vector math
- **GLSL** — custom vertex and fragment shaders with Phong lighting and shadow mapping
- **Swing / AWT** — windowing and UI overlay

---

## Project Structure

```
SolarSystem/
├── code/
│   ├── Code.java           # Main application class
│   ├── Sphere.java         # Sphere geometry generator
│   ├── Utils.java          # Shader loading, texture utilities
│   ├── vertShader1.glsl    # Shadow pass vertex shader
│   ├── fragShader1.glsl    # Shadow pass fragment shader
│   ├── vertShader2.glsl    # Main render vertex shader
│   └── fragShader2.glsl    # Main render fragment shader
└── textures/
    ├── sun.jpg
    ├── mercury.jpg
    ├── venus.jpg
    ├── earth.jpg
    ├── mars.jpg
    ├── jupiter.jpg
    ├── saturn.jpg
    ├── uranus.jpg
    ├── neptune.jpg
    ├── pluto.jpg
    ├── moon.jpg
    ├── io.jpg
    ├── europa.jpg
    ├── ganymede.jpg
    ├── callisto.jpg
    ├── skydome.png
    ├── asteroidBelt.png
    └── saturnsRings.png
```

> ⚠️ **Textures are not included in this repository.** See the [Textures](#-textures) section below.

---

## Setup & Installation

### Prerequisites

- Java JDK 11 or higher
- [JOGL](https://jogamp.org/deployment/jogamp-current/archive/) — download the full platform bundle
- [JOML](https://github.com/JOML-CI/JOML/releases) — download the latest `.jar`

### Required JARs

Add all of the following to your classpath:

| JAR | Source |
|-----|--------|
| `jogl-all.jar` | JogAmp |
| `jogl-all-natives-windows-amd64.jar` | JogAmp (or your platform) |
| `gluegen-rt.jar` | JogAmp |
| `gluegen-rt-natives-windows-amd64.jar` | JogAmp (or your platform) |
| `joml-x.x.x.jar` | JOML GitHub releases |

> If you're on Mac or Linux, replace `windows-amd64` with your platform (e.g. `linux-amd64`, `macosx-universal`).

### Running in jGRASP

1. Go to **Settings → PATH/CLASSPATH → Workspace → CLASSPATH**
2. Add all JARs listed above
3. Open `Code.java` and click **Run**

### Running from the Command Line

```bash
javac -cp ".;jogl-all.jar;gluegen-rt.jar;joml-x.x.x.jar" code/Code.java code/Sphere.java code/Utils.java

java -cp ".;jogl-all.jar;jogl-all-natives-windows-amd64.jar;gluegen-rt.jar;gluegen-rt-natives-windows-amd64.jar;joml-x.x.x.jar" code.Code
```

> On Mac/Linux use `:` instead of `;` as the classpath separator.

---

## Textures

Textures are **not included** in this repository due to copyright. You can find free planet textures at:

- [Solar System Scope Textures](https://www.solarsystemscope.com/textures/) ← recommended
- [NASA Visible Earth](https://visibleearth.nasa.gov/)
- [JHT's Planetary Pixel Emporium](https://planetpixelemporium.com/planets.html)

Once downloaded, rename them to match the filenames listed in the project structure above and place them in the `textures/` folder.

---

## Controls

| Key | Action |
|-----|--------|
| `T` | Toggle between Top-Down and Navigation mode |
| `V` | Toggle scaled / realistic orbital view *(Top-Down only)* |
| `R` | Play / Pause time |
| `→` | Fly to next planet *(Navigation mode)* |
| `0` | Jump to Sun |
| `1` | Jump to Mercury |
| `2` | Jump to Venus |
| `3` | Jump to Earth |
| `4` | Jump to Mars |
| `5` | Jump to Jupiter |
| `6` | Jump to Saturn |
| `7` | Jump to Uranus |
| `8` | Jump to Neptune |
| `9` | Jump to Pluto |

---

## Known Limitations 
- Orbital inclinations are not simulated (all planets orbit on the same plane)
- Planet sizes are not to scale with distances in realistic mode

--- 
## Author 

#### Kaitlyn Morris | [LinkedIn](http://www.linkedin.com/in/kaitlyn-morris-) 
---
*Licensed under the [MIT License](LICENSE).*
