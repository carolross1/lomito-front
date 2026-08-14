import os
import glob

# Rutas de los módulos
base_dir = r"c:\Users\ANDY\Documents\UTNG\INGENIERIA\9no cuatrimestre\Desarrollo para Dispositivos Inteligentes\Unidad II\Proyecto Lomito\lomito-front"
modules = {
    "mobile": {
        "title": "Móvil (Android Smartphone)",
        "project": "Lomito Seguro",
        "output": os.path.join(base_dir, "documentacion", "README_MOBILE.md")
    },
    "tv": {
        "title": "TV (Android TV)",
        "project": "Lomito Seguro",
        "output": os.path.join(base_dir, "documentacion", "README_TV.md")
    },
    "wear": {
        "title": "Wear (Wear OS)",
        "project": "Lomito Seguro",
        "output": os.path.join(base_dir, "documentacion", "README_WEAR.md")
    }
}

def generate_readme_for_module(module_name, config):
    src_path = os.path.join(base_dir, module_name, "src", "main", "java")
    if not os.path.exists(src_path):
        print(f"Directory not found: {src_path}")
        return

    # Gather all .kt files
    kt_files = []
    for root, dirs, files in os.walk(src_path):
        for file in files:
            if file.endswith(".kt"):
                kt_files.append(os.path.join(root, file))

    with open(config["output"], "w", encoding="utf-8") as f:
        # Escribir el encabezado
        f.write(f"# Guía Paso a Paso: Construyendo el Módulo {config['title']} de {config['project']}\n\n")
        f.write(f"Esta guía documenta y desglosa paso a paso la arquitectura, configuración y construcción completa del módulo **{config['title']}** de **{config['project']}**, explicando las decisiones técnicas, patrones de diseño y bloques de código esenciales.\n\n")
        f.write("---\n\n")

        # Agrupar por directorios
        dirs_dict = {}
        for kt in kt_files:
            rel_dir = os.path.dirname(os.path.relpath(kt, src_path))
            if rel_dir not in dirs_dict:
                dirs_dict[rel_dir] = []
            dirs_dict[rel_dir].append(kt)

        fase_num = 1
        for rel_dir, files in sorted(dirs_dict.items()):
            dir_name = rel_dir if rel_dir else "Raíz"
            f.write(f"## FASE {fase_num}: {dir_name}\n\n")
            
            paso_num = 1
            for kt_file in sorted(files):
                file_name = os.path.basename(kt_file)
                f.write(f"### Paso {fase_num}.{paso_num}: {file_name}\n\n")
                
                try:
                    with open(kt_file, "r", encoding="utf-8") as kt_f:
                        code = kt_f.read()
                    f.write("```kotlin\n")
                    f.write(code)
                    if not code.endswith("\n"):
                        f.write("\n")
                    f.write("```\n\n")
                except Exception as e:
                    f.write(f"Error al leer el archivo: {e}\n\n")
                
                paso_num += 1
            fase_num += 1

    print(f"Generated {config['output']}")

for module_name, config in modules.items():
    generate_readme_for_module(module_name, config)

print("¡Listo! Todos los README han sido generados con el código de los archivos.")
