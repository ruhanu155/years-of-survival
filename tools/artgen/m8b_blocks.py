#!/usr/bin/env python3
"""M8 lane B placeholder block models: stoves, stovepipe, storage, cache legs, cellars, smokehouse, shed, lantern.
Cube models on vanilla textures (referenced, not shipped); originals follow in the M8 art pass. Prints the files written."""
import json, os
A = os.path.join(os.path.dirname(__file__), '..', '..', 'src/main/resources/assets/yearofice')
written = []

def w(path, obj):
    path = os.path.normpath(os.path.join(A, path))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    json.dump(obj, open(path, 'w'), indent=1)
    written.append(os.path.relpath(path, os.path.join(A, '..', '..')).replace(os.sep, '/'))

def cube(id, tex): w(f'models/block/{id}.json', {"parent": "minecraft:block/cube_all", "textures": {"all": tex}})
def item(id): w(f'models/item/{id}.json', {"parent": f"yearofice:block/{id}"})

def simple(id, tex):
    cube(id, tex); item(id); w(f'blockstates/{id}.json', {"variants": {"": {"model": f"yearofice:block/{id}"}}})

for id, tex in [('crate', 'minecraft:block/oak_planks'), ('barrel', 'minecraft:block/barrel_side'), ('shelf', 'minecraft:block/spruce_planks'),
                ('hanging_bar', 'minecraft:block/stripped_spruce_log'), ('cache_legs', 'minecraft:block/oak_planks'),
                ('placed_lantern', 'minecraft:block/glowstone'), ('smokehouse', 'minecraft:block/smoker_side'), ('firewood_shed', 'minecraft:block/oak_log')]:
    simple(id, tex)
for id, tex in [('root_cellar', 'minecraft:block/dark_oak_planks'), ('permafrost_cellar', 'minecraft:block/packed_ice')]:
    cube(id, tex); item(id)
    w(f'blockstates/{id}.json', {"variants": {f"facing={f}": {"model": f"yearofice:block/{id}"} for f in ['north', 'south', 'east', 'west']}})
w('models/block/chimney_pipe.json', {"parent": "minecraft:block/block", "textures": {"all": "minecraft:block/iron_block", "particle": "minecraft:block/iron_block"},
  "elements": [{"from": [5, 0, 5], "to": [11, 16, 11], "faces": {k: {"texture": "#all"} for k in ["north", "south", "east", "west", "up", "down"]}}]})
item('chimney_pipe'); w('blockstates/chimney_pipe.json', {"variants": {"": {"model": "yearofice:block/chimney_pipe"}}})
for id, off, on in [('tin_stove', 'minecraft:block/iron_block', 'minecraft:block/furnace_front_on'),
                    ('barrel_stove', 'minecraft:block/gray_concrete', 'minecraft:block/blast_furnace_front_on'),
                    ('masonry_stove', 'minecraft:block/bricks', 'minecraft:block/smoker_front_on')]:
    cube(id, off); cube(id + '_lit', on); item(id)
    w(f'blockstates/{id}.json', {"variants": {"lit=false": {"model": f"yearofice:block/{id}"}, "lit=true": {"model": f"yearofice:block/{id}_lit"}}})
for leg, tex in [('barked', 'minecraft:block/oak_log'), ('peeled', 'minecraft:block/stripped_oak_log'), ('banded', 'minecraft:block/iron_block')]:
    cube('cache_pole_' + leg, tex)
item('cache_pole'); cube('cache_pole', 'minecraft:block/oak_log')
w('blockstates/cache_pole.json', {"variants": {f"leg={l}": {"model": f"yearofice:block/cache_pole_{l}"} for l in ['barked', 'peeled', 'banded']}})
if __name__ == '__main__':
    print(json.dumps(sorted(set(written))))
