# Solar System Visualization

An interactive 3D visualization of our Solar System built with Java and OpenGL (JOGL). Features realistic planetary motion, shadow mapping, and dual viewing modes.

## Features

- **Dual View Modes**
  - Navigation Mode: Fly through the solar system and visit each planet up close
  - Top-Down Mode: View the entire solar system from above with realistic or scaled distances

- **Advanced Graphics**
  - Real-time shadow mapping for Earth and Jupiter's moons
  - Textured planets, moons, and rings
  - Saturn's iconic ring system
  - Asteroid and Kuiper belts
  - High-resolution space skybox

- **Interactive Controls**
  - Smooth camera transitions between celestial bodies
  - Time control (pause/play orbital motion)
  - Direct planet navigation (jump to any planet)
  - Toggle between viewing modes


## Technologies Used

- **Java** - Core programming language
- **JOGL (Java OpenGL)** - OpenGL bindings for 3D rendering
- **JOML** - Java mathematics library for 3D transformations
- **GLSL** - Shader programming for lighting and shadows

## Requirements

- Java JDK 8 or higher
- JOGL libraries (included in `lib/` folder)
- JOML library

## Installation

1. Clone the repository:
```bash
git clone https://github.com/yourusername/solar-system-visualization.git
cd solar-system-visualization
```

2. Ensure JOGL and JOML libraries are in your classpath

3. Compile and run:
```bash
javac -cp "lib/*:." src/code/Code.java
java -cp "lib/*:src" code.Code
```

## Controls

### Navigation Mode
- **[T]** - Toggle between Navigation and Top-Down modes
- **[→]** - Fly to next planet (or skip current transition)
- **[0-9]** - Jump directly to a celestial body:
  - 0: Sun
  - 1: Mercury
  - 2: Venus
  - 3: Earth (with Moon)
  - 4: Mars
  - 5: Jupiter (with Galilean moons)
  - 6: Saturn (with rings)
  - 7: Uranus
  - 8: Neptune
  - 9: Pluto
- **[R]** - Pause/Resume time

### Top-Down Mode
- **[T]** - Toggle to Navigation mode
- **[V]** - Switch between Realistic and Scaled views
- **[R]** - Pause/Resume planetary orbits

## Technical Highlights

### Graphics Features
- **Shadow Mapping**: Implements two-pass rendering for realistic moon shadows on Earth and Jupiter
- **Shader Programming**: Custom GLSL vertex and fragment shaders for lighting calculations
- **Texture Mapping**: High-resolution textures applied to all celestial bodies
- **Smooth Transitions**: Interpolated camera movement with smoothstep function

### Rendering Pipeline
1. **Shadow Pass**: Renders scene from light's perspective to generate shadow map
2. **Main Pass**: Renders full scene with lighting, textures, and shadow application

### Orbital Mechanics
- Realistic orbital speeds (relative to actual planetary periods)
- Proper orbital radii with toggleable scaling for visualization
- Continuous rotation for planets and moons

## Project Structure

- `Code.java` - Main application class with rendering logic
- `vertShader1.glsl` / `fragShader1.glsl` - Shadow pass shaders
- `vertShader2.glsl` / `fragShader2.glsl` - Main rendering shaders
- `Utils.java` - Utility class for shader compilation and texture loading
- `Sphere.java` - Procedural sphere geometry generation

## Educational Information

When viewing each planet in Navigation Mode, you'll see fascinating facts about:
- Physical characteristics
- Unique features
- Moons and ring systems
- Position in the solar system

## Known Limitations

- Orbital inclinations are not simulated (all planets orbit on the same plane)
- Planet sizes are not to scale with distances in realistic mode
- Texture quality depends on available source images


## Acknowledgments

- Planetary textures from [Solar System Scope](https://www.solarsystemscope.com/textures/)
- Created as an honors project for CS 465 (Computer Graphics)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Author

**Kaitlyn Morris**

Feel free to reach out with questions or suggestions!
```
bin/
out/
.idea/
*.iml
