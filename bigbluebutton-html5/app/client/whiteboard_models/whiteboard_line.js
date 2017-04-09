let MAX_PATHS_IN_SEQUENCE = 30;

this.WhiteboardLineModel = (function() {

  // A line in the whiteboard
  // Note: is used to draw lines from the pencil tool and from the line tool, this is why some
  // methods can receive different set of parameters.
  // TODO: Maybe this should be split in WhiteboardPathModel for the pencil and
  //       WhiteboardLineModel for the line tool
  class WhiteboardLineModel extends WhiteboardToolModel {
    constructor(paper) {
      super(paper);
      this.paper = paper;

      // the defintion of this shape, kept so we can redraw the shape whenever needed
      // format: svg path, stroke color, thickness
      this.definition = ["", "#000", "0px"];
    }

    // Creates a line in the paper
    // @param  {number} x         the x value of the line start point as a percentage of the original width
    // @param  {number} y         the y value of the line start point as a percentage of the original height
    // @param  {string} colour    the colour of the shape to be drawn
    // @param  {number} thickness the thickness of the line to be drawn
    make(info) {
      let color, path, pathPercent, thickness, x, x1, y, y1;
      if((info != null ? info.points : void 0) != null) {
        x = info.points[0];
        y = info.points[1];
        color = info.color;
        thickness = info.thickness;
        x1 = x * this.gw + this.xOffset;
        y1 = y * this.gh + this.yOffset;
        path = `M${x1} ${y1} L${x1} ${y1}`;
        pathPercent = `M${x} ${y} L${x} ${y}`;
        this.obj = this.paper.path(path);
        this.obj.attr("stroke", formatColor(color));
        this.obj.attr("stroke-width", zoomStroke(formatThickness(thickness)));
        this.obj.attr({
          "stroke-linejoin": "round"
        });
        this.obj.attr("stroke-linecap", "round");
        this.definition = [pathPercent, this.obj.attrs["stroke"], this.obj.attrs["stroke-width"]];
      }
      return this.obj;
    }

    // Update the line dimensions
    // @param  {number}  x1         1) the x of the first point
    //                              2) the next x point to be added to the line
    // @param  {number}  y1         1) the y of the first point
    //                              2) the next y point to be added to the line
    // @param  {number,boolean} x2  1) the x of the second point
    //                              2) true if the line should be added to the current line,
    //                                 false if it should replace the last point
    // @param  {number}         y2  1) the y of the second point
    //                              2) undefined
    update(info) {
      let path, x1, x2, y1, y2;
      if((info != null ? info.points : void 0) != null) {
        x1 = info.points[0];
        y1 = info.points[1];
        x2 = info.points[2];
        y2 = info.points[3];
        if(this.obj != null) {
          path = this._buildPath(info.points);
          this.definition[0] = path;
          path = this._scaleLinePath(path, this.gw, this.gh, this.xOffset, this.yOffset);
          return this.obj.attr({
            path: path
          });
        }
      }
    }

    // Draw a line on the paper
    // @param  {number,string} x1 1) the x value of the first point
    //                            2) the string path
    // @param  {number,string} y1 1) the y value of the first point
    //                            2) the colour
    // @param  {number} x2        1) the x value of the second point
    //                            2) the thickness
    // @param  {number} y2        1) the y value of the second point
    //                            2) undefined
    // @param  {string} colour    1) the colour of the shape to be drawn
    //                            2) undefined
    // @param  {number} thickness 1) the thickness of the line to be drawn
    //                            2) undefined
    draw(x1, y1, x2, y2, colour, thickness) {
      // if the drawing is from the pencil tool, it comes as a path first
      // if _.isString(x1)
      //   colour = y1
      //   thickness = x2
      //   path = x1

      // // if the drawing is from the line tool, it comes with two points
      // else
      //   path = @_buildPath(points)

      // line = @paper.path(@_scaleLinePath(path, @gw, @gh, @xOffset, @yOffset))
      // line.attr Utils.strokeAndThickness(colour, thickness)
      // line.attr({"stroke-linejoin": "round"})
      // line
    }

    // When dragging for drawing lines starts
    // @param  {number} x the x value of the cursor
    // @param  {number} y the y value of the cursor
    // TODO: moved here but not finished
    dragOnStart(x, y) {
      // // find the x and y values in relation to the whiteboard
      // sx = (@paperWidth - @gw) / 2
      // sy = (@paperHeight - @gh) / 2
      // @lineX = x - @containerOffsetLeft - sx + @xOffset
      // @lineY = y - @containerOffsetTop - sy + @yOffset
      // values = [ @lineX / @paperWidth, @lineY / @paperHeight, @currentColour, @currentThickness ]
      // globals.connection.emitMakeShape "line", values
    }

    // As line drawing drag continues
    // @param  {number} dx the difference between the x value from _lineDragStart and now
    // @param  {number} dy the difference between the y value from _lineDragStart and now
    // @param  {number} x  the x value of the cursor
    // @param  {number} y  the y value of the cursor
    // TODO: moved here but not finished
    dragOnMove(dx, dy, x, y) {
      // sx = (@paperWidth - @gw) / 2
      // sy = (@paperHeight - @gh) / 2
      // [cx, cy] = @_currentSlideOffsets()
      // // find the x and y values in relation to the whiteboard
      // @cx2 = x - @containerOffsetLeft - sx + @xOffset
      // @cy2 = y - @containerOffsetTop - sy + @yOffset
      // if @shiftPressed
      //   globals.connection.emitUpdateShape "line", [ @cx2 / @paperWidth, @cy2 / @paperHeight, false ]
      // else
      //   @currentPathCount++
      //   if @currentPathCount < MAX_PATHS_IN_SEQUENCE
      //     globals.connection.emitUpdateShape "line", [ @cx2 / @paperHeight, @cy2 / @paperHeight, true ]
      //   else if @obj?
      //     @currentPathCount = 0
      //     // save the last path of the line
      //     @obj.attrs.path.pop()
      //     path = @obj.attrs.path.join(" ")
      //     @obj.attr path: (path + "L" + @lineX + " " + @lineY)

      //     // scale the path appropriately before sending
      //     pathStr = @obj.attrs.path.join(",")
      //     globals.connection.emitPublishShape "path",
      //       [ @_scaleLinePath(pathStr, 1 / @gw, 1 / @gh),
      //         @currentColour, @currentThickness ]
      //     globals.connection.emitMakeShape "line",
      //       [ @lineX / @paperWidth, @lineY / @paperHeight, @currentColour, @currentThickness ]
      //   @lineX = @cx2
      //   @lineY = @cy2
    }

    // Drawing line has ended
    // @param  {Event} e the mouse event
    // TODO: moved here but not finished
    dragOnEnd(e) {
      // if @obj?
      //   path = @obj.attrs.path
      //   @obj = null # any late updates will be blocked by this
      //   // scale the path appropriately before sending
      //   globals.connection.emitPublishShape "path",
      //     [ @_scaleLinePath(path.join(","), 1 / @gw, 1 / @gh),
      //       @currentColour, @currentThickness ]
    }

    _buildPath(points) {
      let i, path;
      path = "";
      if(points && points.length >= 2) {
        path += `M ${points[0]} ${points[1]}`;
        i = 2;
        while(i < points.length) {
          path += `${i}${1}L${points[i]} ${points[i + 1]}`;
          i += 2;
        }
        path += "Z";
        return path;
      }
    }

    // Scales a path string to fit within a width and height of the new paper size
    // @param  {number} w width of the shape as a percentage of the original width
    // @param  {number} h height of the shape as a percentage of the original height
    // @return {string}   the path string after being manipulated to new paper size
    _scaleLinePath(string, w, h, xOffset, yOffset) {
      let j, len, path, points;
      if(xOffset == null) {
        xOffset = 0;
      }
      if(yOffset == null) {
        yOffset = 0;
      }
      path = null;
      points = string.match(/(\d+[.]?\d*)/g);
      len = points.length;
      j = 0;

      // go through each point and multiply it by the new height and width
      while(j < len) {
        if(j !== 0) {
          path += `${points[j + 1] * h}${yOffset}L${points[j] * w + xOffset},${points[j + 1] * h + yOffset}`;
        } else {
          path = `${points[j + 1] * h}${yOffset}M${points[j] * w + xOffset},${points[j + 1] * h + yOffset}`;
        }
        j += 2;
      }
      return path;
    }
  }

  return WhiteboardLineModel;
})();
