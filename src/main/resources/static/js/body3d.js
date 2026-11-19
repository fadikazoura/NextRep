import {
    WebGLRenderer, Scene, PerspectiveCamera, Group, Mesh, SphereGeometry, CapsuleGeometry, CylinderGeometry,
    CircleGeometry, MeshStandardMaterial, ShadowMaterial, HemisphereLight, DirectionalLight, Color, Raycaster,
    Vector2, PCFShadowMap, SRGBColorSpace, ACESFilmicToneMapping,
} from './vendor/three.min.js';

/*
 * NextRep 3D muscle map: a stylised training mannequin built from simple shapes (no model file to download).
 * Every muscle group is its own mesh, so it can be clicked and colored by training volume.
 * Units: the figure is ~4.2 tall and stands on y = 0, facing +z.
 */

export const MUSCLE_GROUPS = ['CHEST', 'BACK', 'SHOULDERS', 'BICEPS', 'TRICEPS', 'QUADS', 'HAMSTRINGS', 'GLUTES', 'CALVES', 'CORE'];

const SPHERE = new SphereGeometry(1, 40, 28);

function cssColor(name, fallback) {
    const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
    return new Color(value || fallback);
}

/** Proportions differ a little between the male and female figure. */
function proportions(sex) {
    const female = sex === 'FEMALE';
    return {
        shoulder: female ? 0.56 : 0.64,
        chestWidth: female ? 0.48 : 0.56,
        waist: female ? 0.36 : 0.42,
        hip: female ? 0.47 : 0.44,
        arm: female ? 0.065 : 0.075,
        thigh: female ? 0.13 : 0.12,
        muscle: female ? 0.85 : 1,
        female,
    };
}

export function createBody3d(container, { sex = 'MALE', onSelect } = {}) {
    const p = proportions(sex);
    const accent = cssColor('--accent', '#c6ff3d');
    const skin = new Color('#59606d');
    const idle = new Color('#8b94a3');
    const fabric = new Color('#14171c');

    /* ---------- renderer, scene, camera ---------- */
    const renderer = new WebGLRenderer({ antialias: true, alpha: true });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.outputColorSpace = SRGBColorSpace;
    renderer.toneMapping = ACESFilmicToneMapping;
    renderer.shadowMap.enabled = true;
    renderer.shadowMap.type = PCFShadowMap;
    renderer.domElement.className = 'body3d-canvas';
    container.append(renderer.domElement);

    const scene = new Scene();
    const camera = new PerspectiveCamera(30, 1, 0.1, 100);
    camera.position.set(0, 2.15, 8.9);
    camera.lookAt(0, 2.08, 0);

    scene.add(new HemisphereLight(0xffffff, 0x222222, 1.4));
    const key = new DirectionalLight(0xffffff, 2.2);
    key.position.set(3, 6, 5);
    key.castShadow = true;
    key.shadow.mapSize.set(1024, 1024);
    key.shadow.camera.left = -3;
    key.shadow.camera.right = 3;
    key.shadow.camera.top = 5;
    key.shadow.camera.bottom = -1;
    key.shadow.radius = 6;
    scene.add(key);
    const rim = new DirectionalLight(accent, 2.4); // gym-style colored back light
    rim.position.set(-4, 4, -5);
    scene.add(rim);
    const fill = new DirectionalLight(0xbfd4ff, 0.6);
    fill.position.set(-5, 2, 4);
    scene.add(fill);

    const floor = new Mesh(new CircleGeometry(1.6, 48), new ShadowMaterial({ opacity: 0.35 }));
    floor.rotation.x = -Math.PI / 2;
    floor.receiveShadow = true;
    scene.add(floor);

    /* ---------- the figure ---------- */
    const figure = new Group();
    scene.add(figure);

    const skinMaterial = new MeshStandardMaterial({ color: skin, roughness: 0.5, metalness: 0.2 });
    const fabricMaterial = new MeshStandardMaterial({ color: fabric, roughness: 0.85, metalness: 0 });
    const muscleMaterials = Object.fromEntries(MUSCLE_GROUPS.map((group) => [group,
        new MeshStandardMaterial({ color: idle.clone(), roughness: 0.35, metalness: 0.15, emissive: accent, emissiveIntensity: 0 })]));
    const muscleMeshes = [];

    const add = (mesh, x, y, z, { rx = 0, ry = 0, rz = 0 } = {}) => {
        mesh.position.set(x, y, z);
        mesh.rotation.set(rx, ry, rz);
        mesh.castShadow = true;
        figure.add(mesh);
        return mesh;
    };
    const ellipsoid = (material, sx, sy, sz) => {
        const mesh = new Mesh(SPHERE, material);
        mesh.scale.set(sx, sy, sz);
        return mesh;
    };
    const capsule = (radius, length, material = skinMaterial) => new Mesh(new CapsuleGeometry(radius, length, 8, 24), material);
    /** Adds a part on both sides of the body (x and -x). */
    const both = (make, x, y, z, rot = {}) => {
        add(make(), x, y, z, rot);
        add(make(), -x, y, z, { ...rot, ry: -(rot.ry ?? 0), rz: -(rot.rz ?? 0) });
    };
    const muscle = (group, sx, sy, sz, x, y, z, rot = {}, mirrored = true) => {
        const make = () => {
            const mesh = ellipsoid(muscleMaterials[group], sx * p.muscle, sy, sz * p.muscle);
            mesh.userData.muscle = group;
            muscleMeshes.push(mesh);
            return mesh;
        };
        if (mirrored) both(make, x, y, z, rot);
        else add(make(), x, y, z, rot);
    };

    // base body ("skin")
    add(ellipsoid(skinMaterial, 0.19, 0.24, 0.21), 0, 3.9, 0.02); // head
    add(new Mesh(new CylinderGeometry(0.1, 0.13, 0.26, 24), skinMaterial), 0, 3.6, -0.01); // neck
    add(ellipsoid(skinMaterial, p.chestWidth * 0.9, 0.46, 0.21), 0, 3.08, 0); // ribcage
    add(ellipsoid(skinMaterial, p.waist * 0.92, 0.42, 0.19), 0, 2.55, 0); // waist
    add(ellipsoid(fabricMaterial, p.hip, 0.27, 0.25), 0, 2.08, 0); // pelvis, in training shorts
    both(() => capsule(0.165, 0.18, fabricMaterial), 0.22, 1.88, 0, { rz: 0.03 }); // shorts legs
    both(() => ellipsoid(skinMaterial, 0.13, 0.11, 0.13), p.shoulder - 0.08, 3.37, 0); // shoulder joints
    both(() => capsule(p.arm, 0.5), p.shoulder + 0.02, 2.98, 0, { rz: 0.1 }); // upper arms
    both(() => capsule(p.arm * 0.82, 0.48), p.shoulder + 0.09, 2.38, 0.04, { rz: 0.06, rx: -0.08 }); // forearms
    both(() => ellipsoid(skinMaterial, 0.06, 0.11, 0.08), p.shoulder + 0.12, 1.98, 0.07); // hands
    both(() => capsule(p.thigh, 0.72), 0.22, 1.42, 0, { rz: 0.03 }); // thighs
    both(() => ellipsoid(skinMaterial, 0.11, 0.1, 0.11), 0.235, 0.96, 0.02); // knees
    both(() => capsule(0.075, 0.66), 0.24, 0.55, 0); // shins
    both(() => ellipsoid(skinMaterial, 0.085, 0.06, 0.19), 0.25, 0.06, 0.07); // feet
    if (p.female) both(() => ellipsoid(skinMaterial, 0.14, 0.13, 0.11), 0.14, 3.06, 0.14); // bust

    // muscles — front
    if (!p.female) muscle('CHEST', 0.25, 0.18, 0.12, p.chestWidth * 0.42, 3.16, 0.12, { ry: 0.3, rz: -0.08 });
    else muscle('CHEST', 0.2, 0.08, 0.08, p.chestWidth * 0.42, 3.27, 0.18, { ry: 0.3 });
    muscle('SHOULDERS', 0.18, 0.18, 0.19, p.shoulder - 0.01, 3.35, 0.0);
    muscle('BICEPS', 0.085, 0.22, 0.085, p.shoulder + 0.03, 2.98, 0.04, { rz: 0.1 });
    for (const y of [2.84, 2.64, 2.44]) muscle('CORE', 0.088, 0.092, 0.07, 0.093, y, 0.15); // six-pack
    muscle('CORE', 0.1, 0.26, 0.11, p.waist * 0.7, 2.56, 0.06, { rz: -0.15 }); // obliques
    muscle('QUADS', 0.15, 0.4, 0.13, 0.22, 1.42, 0.035, { rz: 0.03 });

    // muscles — back
    muscle('BACK', 0.34, 0.15, 0.12, 0, 3.42, -0.08, {}, false); // traps
    muscle('BACK', 0.24, 0.42, 0.11, p.chestWidth * 0.42, 2.9, -0.11, { rz: -0.2, ry: -0.3 }); // lats
    muscle('BACK', 0.09, 0.22, 0.08, 0.09, 2.42, -0.13); // lower back
    muscle('TRICEPS', 0.09, 0.25, 0.085, p.shoulder + 0.03, 3.0, -0.04, { rz: 0.1 });
    muscle('GLUTES', 0.2, 0.2, 0.14, 0.19, 1.98, -0.12);
    muscle('HAMSTRINGS', 0.145, 0.38, 0.12, 0.22, 1.42, -0.035, { rz: 0.03 });
    muscle('CALVES', 0.1, 0.25, 0.1, 0.24, 0.72, -0.03);

    /* ---------- state: heat colors, hover, selection ---------- */
    let heat = {};
    let hovered = null;
    let selected = null;

    const paint = () => {
        for (const group of MUSCLE_GROUPS) {
            const level = Math.min(1, (heat[group] ?? 0) / 12); // ~12 sets a week = fully lit
            const material = muscleMaterials[group];
            material.color.copy(idle).lerp(accent, level > 0 ? 0.35 + level * 0.65 : 0);
            material.emissiveIntensity = group === selected ? 0.55 : group === hovered ? 0.25 : level * 0.12;
        }
        requestRender();
    };

    /* ---------- interaction: drag to rotate, tap to select ---------- */
    const raycaster = new Raycaster();
    const pointer = new Vector2();
    const reduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
    let autoRotate = !reduceMotion;
    let velocity = 0;
    let drag = null;

    const pick = (event) => {
        const rect = renderer.domElement.getBoundingClientRect();
        pointer.set(((event.clientX - rect.left) / rect.width) * 2 - 1, -((event.clientY - rect.top) / rect.height) * 2 + 1);
        raycaster.setFromCamera(pointer, camera);
        const hit = raycaster.intersectObjects(figure.children, false)[0];
        return hit?.object.userData.muscle ?? null;
    };

    const canvas = renderer.domElement;
    canvas.addEventListener('pointerdown', (event) => {
        drag = { x: event.clientX, startX: event.clientX, startY: event.clientY, time: performance.now() };
        autoRotate = false;
        canvas.setPointerCapture(event.pointerId);
    });
    canvas.addEventListener('pointermove', (event) => {
        if (drag) {
            const dx = event.clientX - drag.x;
            drag.x = event.clientX;
            figure.rotation.y += dx * 0.012;
            velocity = dx * 0.012;
            requestRender();
        } else if (event.pointerType === 'mouse') {
            const group = pick(event);
            if (group !== hovered) {
                hovered = group;
                canvas.style.cursor = group ? 'pointer' : 'grab';
                paint();
            }
        }
    });
    canvas.addEventListener('pointerup', (event) => {
        const moved = Math.hypot(event.clientX - drag.startX, event.clientY - drag.startY);
        drag = null;
        if (moved < 6) {
            velocity = 0;
            const group = pick(event);
            if (group) {
                selected = group;
                paint();
                onSelect?.(group);
            }
        }
    });
    canvas.addEventListener('pointercancel', () => { drag = null; });
    canvas.addEventListener('pointerleave', () => {
        if (hovered) {
            hovered = null;
            paint();
        }
    });

    /* ---------- render loop: only runs while visible and something moves ---------- */
    let frame = 0;
    let visible = true;
    let needsRender = true;

    function requestRender() {
        needsRender = true;
        if (!frame && visible) frame = requestAnimationFrame(loop);
    }

    function loop() {
        frame = 0;
        if (!canvas.isConnected) {
            dispose();
            return;
        }
        let moving = false;
        if (!drag && Math.abs(velocity) > 0.0005) {
            figure.rotation.y += velocity;
            velocity *= 0.93; // inertia after a swipe
            moving = true;
        } else if (autoRotate) {
            figure.rotation.y += 0.004;
            moving = true;
        }
        if (moving || needsRender) {
            renderer.render(scene, camera);
            needsRender = false;
        }
        if (moving && visible) frame = requestAnimationFrame(loop);
    }

    const resize = () => {
        const width = container.clientWidth || 300;
        const height = Math.round(width * 1.25);
        renderer.setSize(width, height, false);
        canvas.style.width = '100%';
        canvas.style.height = `${height}px`;
        camera.aspect = width / height;
        camera.updateProjectionMatrix();
        requestRender();
    };
    const resizeObserver = new ResizeObserver(resize);
    resizeObserver.observe(container);
    const visibilityObserver = new IntersectionObserver(([entry]) => {
        visible = entry.isIntersecting;
        if (visible) requestRender();
    });
    visibilityObserver.observe(container);

    function dispose() {
        cancelAnimationFrame(frame);
        resizeObserver.disconnect();
        visibilityObserver.disconnect();
        renderer.dispose();
    }

    resize();
    paint();
    if (autoRotate) requestRender();

    return {
        setHeat(volume) {
            heat = volume ?? {};
            paint();
        },
        setSelected(group) {
            selected = group;
            paint();
        },
        /** Turns the figure to face front (0) or back (π). */
        face(side) {
            autoRotate = false;
            velocity = 0;
            const target = side === 'back' ? Math.PI : 0;
            const start = figure.rotation.y;
            const turns = Math.round((start - target) / (2 * Math.PI));
            const goal = target + turns * 2 * Math.PI;
            const began = performance.now();
            const step = (now) => {
                const k = Math.min(1, (now - began) / 500);
                figure.rotation.y = start + (goal - start) * (1 - (1 - k) ** 3);
                renderer.render(scene, camera);
                if (k < 1) requestAnimationFrame(step);
            };
            requestAnimationFrame(step);
        },
        dispose,
    };
}

/** True when the browser can draw WebGL (otherwise the flat SVG map is used). */
export function webglAvailable() {
    try {
        const canvas = document.createElement('canvas');
        return Boolean(window.WebGLRenderingContext && (canvas.getContext('webgl2') || canvas.getContext('webgl')));
    } catch {
        return false;
    }
}
