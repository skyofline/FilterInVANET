/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import java.util.List;

import movement.map.DijkstraPathFinder;
import movement.map.MapNode;
import core.Coord;
import core.Settings;

/**
 * The CarMovement class representing the car movement submodel
 *
 * @author Frans Ekman
 */
public class CarMovement extends MapBasedMovement implements
	SwitchableMovement, TransportMovement {

	private Coord from;
	private Coord to;

	private DijkstraPathFinder pathFinder;

	/**
	 * Car movement constructor
	 * @param settings
	 */
	public CarMovement(Settings settings) {
		super(settings);
		System.out.println("using carmovement");
		pathFinder = new DijkstraPathFinder(getOkMapNodeTypes());
	}

	/**
	 * Construct a new CarMovement instance from a prototype
	 * @param proto
	 */
	public CarMovement(CarMovement proto) {
		super(proto);
		System.out.println("using carmovement");

		this.pathFinder = proto.pathFinder;
	}

	/**
	 * Sets the next route to be taken
	 * @param nodeLocation
	 * @param nodeDestination
	 */
	public void setNextRoute(Coord nodeLocation, Coord nodeDestination) {
		from = nodeLocation.clone();
		to = nodeDestination.clone();
	}

	@Override
	public Path getPath() {
		System.out.println("testing4==========");
		Path path = new Path(generateSpeed());

		MapNode fromNode = getMap().getNodeByCoord(from);
		MapNode toNode = getMap().getNodeByCoord(to);

		List<MapNode> nodePath = pathFinder.getShortestPath(fromNode, toNode);

		for (MapNode node : nodePath) { // create a Path from the shortest path
			path.addWaypoint(node.getLocation());
		}

		lastMapNode = toNode;

		return path;
	}
	/*
	 * 新添加的path功能，用于根据已有的tracks中的起始点来生成path
	 */
	public Path getPath(Coord fromC,Coord toC) {
		System.out.println("testing.....................3");
		Path path = new Path(generateSpeed());

		MapNode fromNode = getMap().getNodeByCoord(fromC);
		MapNode toNode = getMap().getNodeByCoord(toC);

		List<MapNode> nodePath = pathFinder.getShortestPath(fromNode, toNode);

		for (MapNode node : nodePath) { // create a Path from the shortest path
			path.addWaypoint(node.getLocation());
		}

		lastMapNode = toNode;

		return path;
	}
	/**
	 * @see SwitchableMovement
	 * @return true
	 */
	public boolean isReady() {
		return true;
	}
}
