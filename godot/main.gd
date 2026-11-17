extends Node3D

const NX := 25
const NY := 30
const SIZE := 0.27
const TOTAL := NX * NY
var depth := PackedByteArray()
var money := 0
var tool := 1
var site := 1
var excavated := 0
var dragging := false
var sand_mesh: MeshInstance3D
var last_dig_ms := 0
var discovery_count := 0
var started_at := 0
var hud_panel: PanelContainer
var camera: Camera3D
var hud: CanvasLayer
var title_label: Label
var status_label: Label
var toast_label: Label
var buy_button: Button
var next_button: Button
var dust: Array[Node3D] = []
var found := [false, false, false, false]
const REWARDS := [80, 110, 160, 260]
const NAMES := ["ANCIENT RELIC", "ROBOT ARM", "ENERGY CORE", "ROBOT AWAKENED"]
const SPOTS := [Vector2i(5, 7), Vector2i(18, 12), Vector2i(8, 22), Vector2i(16, 25)]

func _ready() -> void:
 depth.resize(TOTAL)
 _load_progress()
 started_at = Time.get_ticks_msec()
 _setup_environment()
 _create_robot()
 _setup_sand()
 _setup_hud()
 _refresh()

func _mat(color: Color, metallic := 0.0) -> StandardMaterial3D:
 var m := StandardMaterial3D.new()
 m.albedo_color = color
 m.metallic = metallic
 m.roughness = 0.75
 return m

func _cube(parent: Node3D, pos: Vector3, scale: Vector3, color: Color, metal := 0.0) -> MeshInstance3D:
 var node := MeshInstance3D.new()
 var mesh := BoxMesh.new()
 mesh.size = scale
 node.mesh = mesh
 node.material_override = _mat(color, metal)
 node.position = pos
 parent.add_child(node)
 return node

func _setup_environment() -> void:
 var env := WorldEnvironment.new()
 var environment := Environment.new()
 environment.background_mode = Environment.BG_COLOR
 environment.background_color = Color("#223b48")
 environment.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
 environment.ambient_light_color = Color("#ffe4b4")
 environment.ambient_light_energy = 0.9
 env.environment = environment
 add_child(env)
 var light := DirectionalLight3D.new()
 light.rotation_degrees = Vector3(-48, -35, 0)
 light.light_energy = 1.4
 add_child(light)
 camera = Camera3D.new()
 camera.position = Vector3(0, 8.3, 7.8)
 camera.fov = 55
 add_child(camera)
 camera.look_at(Vector3(0, -0.1, 0), Vector3.UP)
 camera.make_current()
 _cube(self, Vector3(0,-1.5,0), Vector3(8,0.4,9), Color("#614735"))
 for i in range(10):
  var x := -3.8 if i % 2 == 0 else 3.8
  _cube(self, Vector3(x,-0.5,-3.7+i*0.8), Vector3(0.15,1.0,0.5), Color("#b78a53"))

func _create_robot() -> void:
 var robot := Node3D.new()
 robot.name = "BuriedRobot"
 add_child(robot)
 _cube(robot, Vector3(0,-0.65,0.4), Vector3(2.0,0.5,2.7), Color("#546b76"),0.45)
 _cube(robot, Vector3(0,-0.36,-0.4), Vector3(1.65,0.42,1.4), Color("#8d9fa1"),0.55)
 _cube(robot, Vector3(-0.43,-0.12,-0.72), Vector3(0.45,0.12,0.25), Color("#4af6e6"))
 _cube(robot, Vector3(0.43,-0.12,-0.72), Vector3(0.45,0.12,0.25), Color("#4af6e6"))
 _cube(robot, Vector3(0,-0.12,-0.06), Vector3(0.45,0.11,0.16), Color("#2b414b"))
 _cube(robot, Vector3(-1.36,-0.68,0.65), Vector3(0.6,0.48,1.7), Color("#687f87"),0.4)
 _cube(robot, Vector3(1.36,-0.68,0.65), Vector3(0.6,0.48,1.7), Color("#687f87"),0.4)
 _cube(robot, Vector3(0,-0.16,1.1), Vector3(0.75,0.18,0.75), Color("#49d4cb"),0.25)
 for i in range(7):
  _cube(robot, Vector3(-0.8+i*0.26,-0.12,1.5),Vector3(0.12,0.1,0.12),Color("#d5b575"),0.4)

func _setup_sand() -> void:
 sand_mesh = MeshInstance3D.new()
 sand_mesh.name = "ExcavationTerrain"
 add_child(sand_mesh)
 _update_sand()

func _sand_height(x: int, y: int) -> float:
 var xx := clampi(x,0,NX-1)
 var yy := clampi(y,0,NY-1)
 var d := float(depth[yy*NX+xx])
 return -1.05 + d*0.48 + sin(float(xx)*0.48+float(site))*0.055 + cos(float(yy)*0.41)*0.055

func _update_sand() -> void:
 var st := SurfaceTool.new()
 st.begin(Mesh.PRIMITIVE_TRIANGLES)
 var origin_x := -float(NX)*SIZE*0.5
 var origin_z := -float(NY)*SIZE*0.5
 for y in range(NY):
  for x in range(NX):
   var px := origin_x+float(x)*SIZE
   var pz := origin_z+float(y)*SIZE
   var h00 := _sand_height(x,y)
   var h10 := _sand_height(x+1,y)
   var h01 := _sand_height(x,y+1)
   var h11 := _sand_height(x+1,y+1)
   var seed := float((x*37+y*17+site*11)%19)/19.0
   var sand_color := Color("#c89a62").lerp(Color("#f3d393"),seed*0.58)
   st.set_color(sand_color)
   st.add_vertex(Vector3(px,h00,pz))
   st.set_color(sand_color)
   st.add_vertex(Vector3(px+SIZE,h10,pz))
   st.set_color(sand_color)
   st.add_vertex(Vector3(px,h01,pz+SIZE))
   st.set_color(sand_color)
   st.add_vertex(Vector3(px+SIZE,h10,pz))
   st.set_color(sand_color)
   st.add_vertex(Vector3(px+SIZE,h11,pz+SIZE))
   st.set_color(sand_color)
   st.add_vertex(Vector3(px,h01,pz+SIZE))
 st.generate_normals()
 var mesh := st.commit()
 var mat := StandardMaterial3D.new()
 mat.vertex_color_use_as_albedo = true
 mat.roughness = 1.0
 mat.cull_mode = BaseMaterial3D.CULL_DISABLED
 mesh.surface_set_material(0,mat)
 sand_mesh.mesh = mesh

func _setup_hud() -> void:
 hud = CanvasLayer.new()
 add_child(hud)
 var ui := Control.new()
 hud.add_child(ui)
 ui.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
 ui.mouse_filter = Control.MOUSE_FILTER_IGNORE
 title_label = Label.new()
 title_label.text = "UNBURIED"
 title_label.add_theme_font_size_override("font_size",42)
 title_label.add_theme_color_override("font_color",Color("#ffe0a3"))
 title_label.position = Vector2(28,30)
 title_label.mouse_filter = Control.MOUSE_FILTER_IGNORE
 ui.add_child(title_label)
 status_label = Label.new()
 status_label.position = Vector2(28,100)
 status_label.add_theme_font_size_override("font_size",24)
 status_label.add_theme_color_override("font_color",Color.WHITE)
 status_label.mouse_filter = Control.MOUSE_FILTER_IGNORE
 ui.add_child(status_label)
 toast_label = Label.new()
 toast_label.position = Vector2(30,180)
 toast_label.add_theme_font_size_override("font_size",25)
 toast_label.add_theme_color_override("font_color",Color("#fff2b4"))
 toast_label.mouse_filter = Control.MOUSE_FILTER_IGNORE
 ui.add_child(toast_label)
 var bar := HBoxContainer.new()
 bar.set_anchors_preset(Control.PRESET_BOTTOM_WIDE)
 bar.offset_top = -120
 bar.offset_bottom = -20
 bar.offset_left = 20
 bar.offset_right = -20
 bar.mouse_filter = Control.MOUSE_FILTER_PASS
 bar.add_theme_constant_override("separation",12)
 ui.add_child(bar)
 buy_button = Button.new()
 buy_button.size_flags_horizontal = Control.SIZE_EXPAND_FILL
 buy_button.add_theme_font_size_override("font_size",21)
 buy_button.pressed.connect(_buy_tool)
 bar.add_child(buy_button)
 next_button = Button.new()
 next_button.size_flags_horizontal = Control.SIZE_EXPAND_FILL
 next_button.add_theme_font_size_override("font_size",21)
 next_button.text = "NEXT SITE"
 next_button.pressed.connect(_next_site)
 bar.add_child(next_button)

func _unhandled_input(event: InputEvent) -> void:
 if event is InputEventScreenTouch:
  dragging = event.pressed
  if dragging:
   _dig_screen(event.position)
 elif event is InputEventScreenDrag and dragging:
  _dig_screen(event.position)
 elif event is InputEventMouseButton:
  dragging = event.pressed
  if dragging:
   _dig_screen(event.position)
 elif event is InputEventMouseMotion and dragging:
  _dig_screen(event.position)

func _dig_screen(pos: Vector2) -> void:
 var origin := camera.project_ray_origin(pos)
 var direction := camera.project_ray_normal(pos)
 if absf(direction.y) < 0.0001:
  return
 var distance := (0.15-origin.y)/direction.y
 if distance < 0:
  return
 var point := origin+direction*distance
 var cx := int(round(point.x/SIZE+float(NX-1)/2.0))
 var cy := int(round(point.z/SIZE+float(NY-1)/2.0))
 var radius := 1 if tool<3 else 2
 if Time.get_ticks_msec()-last_dig_ms < 75:
  return
 last_dig_ms = Time.get_ticks_msec()
 var changed := false
 for y in range(maxi(0,cy-radius),mini(NY,cy+radius+1)):
  for x in range(maxi(0,cx-radius),mini(NX,cx+radius+1)):
   if (x-cx)*(x-cx)+(y-cy)*(y-cy)>radius*radius+1:
    continue
   var idx := y*NX+x
   if depth[idx]>0:
    depth[idx]-=1
    changed = true
    if depth[idx]==0:
     excavated+=1
     if excavated%15==0:
      money+=1
 if changed:
  _update_sand()
  _discover()
  _refresh()
  _save_progress()

func _discover() -> void:
 for i in range(SPOTS.size()):
  if found[i]:
   continue
  var spot: Vector2i = SPOTS[i]
  var count := 0
  var total := 0
  for y in range(maxi(0,spot.y-2),mini(NY,spot.y+3)):
   for x in range(maxi(0,spot.x-2),mini(NX,spot.x+3)):
    total+=1
    if depth[y*NX+x]==0:
     count+=1
  if count>=int(total*0.8):
   found[i]=true
   discovery_count+=1
   money+=REWARDS[i] * site
   toast_label.text = NAMES[i]+"  +"+str(REWARDS[i])
   get_tree().create_timer(2.5).timeout.connect(func():toast_label.text="")

func _buy_tool() -> void:
 var price := _tool_price()
 if tool>=6:
  return
 if money<price or discovery_count < tool:
  if discovery_count < tool:
   toast_label.text = "FIND MORE ARTIFACTS FIRST"
   return
  toast_label.text = "NEED "+str(price)+" COINS"
  return
 money-=price
 tool+=1
 toast_label.text = "TOOL UPGRADED"
 _refresh()
 _save_progress()

func _next_site() -> void:
 if excavated<float(TOTAL)*0.82 or not found.all(func(v): return v):
  toast_label.text = "EXCAVATE 82% AND FIND ALL RELICS"
  return
 site+=1
 excavated=0
 discovery_count=0
 found=[false,false,false,false]
 depth.fill(3)
 _update_sand()
 _refresh()
 _save_progress()

func _refresh() -> void:
 status_label.text = "SITE "+str(site)+"  |  "+str(int(float(excavated)/TOTAL*100.0))+"%  |  "+str(money)+" COINS"
 var price := _tool_price()
 buy_button.text = "MAX TOOL" if tool>=6 else "UPGRADE LV."+str(tool)+"  "+str(price)+" C"
 buy_button.disabled = tool>=6 or money<price or discovery_count<tool
 next_button.disabled = excavated<float(TOTAL)*0.82 or not found.all(func(v): return v)

func _tool_price() -> int:
 return int(250.0 * pow(2.6,float(tool-1)))

func _save_progress() -> void:
 var file := FileAccess.open("user://unburied.save",FileAccess.WRITE)
 if file==null:
  return
 file.store_var({"money":money,"tool":tool,"site":site,"depth":depth,"found":found,"discovery_count":discovery_count})

func _load_progress() -> void:
 depth.fill(3)
 if not FileAccess.file_exists("user://unburied.save"):
  return
 var file := FileAccess.open("user://unburied.save",FileAccess.READ)
 if file==null:
  return
 var data = file.get_var()
 if not data is Dictionary:
  return
 money=int(data.get("money",0))
 tool=clampi(int(data.get("tool",1)),1,6)
 discovery_count=int(data.get("discovery_count",0))
 site=maxi(1,int(data.get("site",1)))
 var stored = data.get("depth",PackedByteArray())
 if stored is PackedByteArray and stored.size()==TOTAL:
  depth=stored
 var flags = data.get("found",[false,false,false,false])
 if flags is Array and flags.size()==4:
  found=flags
  discovery_count=maxi(discovery_count,found.count(true))
 for d in depth:
  if d==0:
   excavated+=1
