#!/usr/bin/env python3
"""生成 VoltCraft 新半成品物品的占位纹理（16x16 像素风格）"""

from PIL import Image, ImageDraw
import os

# 输出路径
output_dir = os.path.join(os.path.dirname(os.path.dirname(__file__)), 'src', 'main', 'resources', 'assets', 'voltcraft', 'textures', 'item')
os.makedirs(output_dir, exist_ok=True)

SIZE = 16

def create_gear():
    """齿轮 - 金属灰，中心有孔，边缘有齿"""
    img = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # 齿轮主体 - 金属灰
    metal_color = (156, 163, 175, 255)  # #9CA3AF
    metal_dark = (107, 114, 128, 255)   # #6B7280
    metal_light = (209, 213, 219, 255)  # #D1D5DB
    hole_color = (31, 41, 55, 255)      # #1F2937

    # 中心圆孔
    draw.rectangle([6, 6, 9, 9], fill=hole_color)

    # 齿轮主体（十字形）
    # 竖条
    draw.rectangle([6, 2, 9, 13], fill=metal_color)
    # 横条
    draw.rectangle([2, 6, 13, 9], fill=metal_color)

    # 齿（四个角的突出）
    # 左上齿
    draw.rectangle([3, 3, 5, 5], fill=metal_color)
    # 右上齿
    draw.rectangle([10, 3, 12, 5], fill=metal_color)
    # 左下齿
    draw.rectangle([3, 10, 5, 12], fill=metal_color)
    # 右下齿
    draw.rectangle([10, 10, 12, 12], fill=metal_color)

    # 高光
    draw.rectangle([7, 3, 8, 4], fill=metal_light)
    draw.rectangle([3, 7, 4, 8], fill=metal_light)

    # 暗面
    draw.rectangle([7, 11, 8, 12], fill=metal_dark)
    draw.rectangle([11, 7, 12, 8], fill=metal_dark)

    img.save(os.path.join(output_dir, 'gear.png'))
    print('Generated gear.png')

def create_electric_motor():
    """电动马达 - 铜色线圈 + 铁色外壳"""
    img = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # 颜色定义
    iron_color = (156, 163, 175, 255)    # 外壳
    copper_color = (180, 120, 60, 255)   # 铜线圈
    copper_light = (220, 160, 80, 255)
    shaft_color = (100, 100, 100, 255)   # 轴

    # 外壳（圆角矩形）
    draw.rectangle([3, 3, 12, 12], fill=iron_color)
    draw.rectangle([4, 4, 11, 11], fill=iron_color)

    # 铜线圈（内部圆形）
    draw.rectangle([5, 5, 10, 10], fill=copper_color)
    draw.rectangle([6, 6, 9, 9], fill=copper_light)

    # 中心轴
    draw.rectangle([7, 7, 8, 8], fill=shaft_color)

    # 接线端子（底部两个点）
    draw.rectangle([5, 13, 6, 14], fill=copper_color)
    draw.rectangle([9, 13, 10, 14], fill=copper_color)

    # 高光
    draw.rectangle([4, 4, 5, 5], fill=(180, 180, 180, 255))

    img.save(os.path.join(output_dir, 'electric_motor.png'))
    print('Generated electric_motor.png')

def create_circuit_board():
    """电路板 - 绿色基板 + 铜走线 + 元件"""
    img = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # 颜色定义
    pcb_green = (34, 139, 34, 255)       # PCB绿色
    pcb_dark = (20, 100, 20, 255)
    copper_trace = (180, 120, 60, 255)   # 铜走线
    solder = (200, 200, 200, 255)        # 焊点
    chip_color = (40, 40, 40, 255)       # 芯片

    # PCB基板
    draw.rectangle([1, 1, 14, 14], fill=pcb_green)
    draw.rectangle([2, 2, 13, 13], fill=pcb_green)

    # 铜走线（横竖）
    draw.rectangle([3, 7, 12, 8], fill=copper_trace)  # 横线
    draw.rectangle([7, 3, 8, 12], fill=copper_trace)  # 竖线

    # 焊点
    for pos in [(3, 3), (12, 3), (3, 12), (12, 12), (7, 7)]:
        draw.rectangle([pos[0], pos[1], pos[0]+1, pos[1]+1], fill=solder)

    # 芯片（中心黑色方块）
    draw.rectangle([6, 5, 9, 10], fill=chip_color)
    draw.rectangle([7, 6, 8, 9], fill=(60, 60, 60, 255))

    # 边缘连接器（底部金色）
    for x in range(3, 13, 2):
        draw.rectangle([x, 13, x+1, 14], fill=(200, 180, 50, 255))

    img.save(os.path.join(output_dir, 'circuit_board.png'))
    print('Generated circuit_board.png')

def create_pump():
    """泵 - 铁色外壳 + 铜色接口"""
    img = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # 颜色定义
    iron_color = (156, 163, 175, 255)
    iron_dark = (107, 114, 128, 255)
    copper_color = (180, 120, 60, 255)
    blade_color = (120, 130, 140, 255)

    # 泵主体（圆形）
    draw.rectangle([3, 3, 12, 12], fill=iron_color)
    draw.rectangle([4, 4, 11, 11], fill=iron_color)

    # 内部腔体
    draw.rectangle([5, 5, 10, 10], fill=iron_dark)

    # 叶轮（十字形）
    draw.rectangle([7, 5, 8, 10], fill=blade_color)
    draw.rectangle([5, 7, 10, 8], fill=blade_color)

    # 中心轴
    draw.rectangle([7, 7, 8, 8], fill=(80, 80, 80, 255))

    # 进出口（铜色）
    draw.rectangle([0, 7, 3, 8], fill=copper_color)   # 左进口
    draw.rectangle([12, 7, 15, 8], fill=copper_color)  # 右出口

    # 高光
    draw.rectangle([4, 4, 5, 5], fill=(180, 180, 180, 255))

    img.save(os.path.join(output_dir, 'pump.png'))
    print('Generated pump.png')

if __name__ == '__main__':
    create_gear()
    create_electric_motor()
    create_circuit_board()
    create_pump()
    print('\nAll item textures generated!')
