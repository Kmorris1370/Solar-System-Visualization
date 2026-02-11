# Solar System Visualization

An interactive 3D solar system simulation built with Java and OpenGL (JOGL). With the planets, moons, and asteroid belts through two distinct viewing modes with real-time shadows and textured celestial bodies.

## Description

Key features include:
- **Two Viewing Modes**: Top-down orthographic view and first-person navigation 
- **Scaled vs Realistic Views**: Toggle between scaled and realistic representations
- **Shadow Mapping**: Shadows cast by moons onto Earth and Jupiter
- **Textured Planets**: Realistic textures for all celestial bodies
- **Saturn's Rings**: Procedurally generated elliptical ring system 
- **Asteroid & Kuiper Belts**: Visual representation of the major belts
- **Time Control**: Pause and resume orbital motion

## Controls

### Global Controls
| Key | Action |
|-----|--------|
| `T` | Toggle between Top-Down and Navigation mode |
| `R` | Play/Pause time  |

### Top-Down Mode
| Key | Action |
|-----|--------|
| `V` | Switch between Scaled and Realistic view |

**Scaled View**: Planets are evenly spaced for easy viewing and exaggerated sizes for visibility.

**Realistic View**: Near true orbital distances with proportionally accurate planet sizes.

### Navigation Mode
| Key | Action |
|-----|--------|
| `→` (Right Arrow) | Fly to next planet / Skip current flight |
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

**Navigation Flow**: Starting at the Sun, press `→` to fly through each planet in order. After Pluto, pressing `→` returns you to the Sun. Use number keys to jump directly to any body.

### Shadows
- Only visible in **Navagation Mode**
- Planets have shadows casted on them as the system rotates
- **Earth** has the the shadow of the moon as it passes by
- **Jupiter** has the shadows of the four Galliean moons as they pass by
- To get a better view of the shadows: **press [R]** to stop time as the moons passes by 


## Running the Project

1. Ensure JOGL and JOML libraries are in your classpath
2. Place all texture files in the project root
3. Compile and run Code.java

## Authors
- Kaitlyn Morris
