package core.actions.cityactions;

import core.Types;
import core.actions.Action;
import core.actors.buildings.Building;
import core.game.Board;
import core.game.GameState;
import core.actors.City;
import utils.Vector2d;

import java.util.LinkedList;

public class Destroy extends CityAction
{
    private Vector2d position;

    public Destroy(City c)
    {
        super.city = c;
    }

    public void setPosition(int x, int y){
        this.position = new Vector2d(x, y);
    }
    public Vector2d getPosition() {
        return position;
    }

    @Override
    public LinkedList<Action> computeActionVariants(final GameState gs) {
        LinkedList<Action> actions = new LinkedList<>();
        Board currentBoard = gs.getBoard();
        LinkedList<Vector2d> tiles = currentBoard.getCityTiles(city.getActorId());
        boolean techReq = gs.getTribe(city.getTribeId()).getTechTree().isResearched(Types.TECHNOLOGY.CONSTRUCTION);
        if (techReq){
            for(Vector2d tile: tiles){
                if (currentBoard.getBuildingAt(tile.x, tile.y) != null){
                    Destroy action = new Destroy(city);
                    action.setPosition(tile.x, tile.y);
                    actions.add(action);
                }
            }
        }
        return actions;
    }

    @Override
    public boolean isFeasible(final GameState gs)
    {
        boolean isBuilding = gs.getBoard().getBuildingAt(position.x, position.y) != null;
        boolean isBelonging = gs.getBoard().getCityIdAt(position.x, position.y) == city.getActorId();
        boolean isResearched = gs.getTribe(city.getTribeId()).getTechTree().isResearched(Types.TECHNOLOGY.CONSTRUCTION);
        return isBuilding && isBelonging && isResearched;
    }

    @Override
    public boolean execute(GameState gs) {
        if (isFeasible(gs)){
            Building removedBuilding = city.removeBuilding(position.x, position.y);
            if (removedBuilding != null) {
                Board b = gs.getBoard();
                b.setBuildingAt(position.x, position.y, null);
                if (removedBuilding.getTYPE() != Types.BUILDING.CUSTOM_HOUSE) {
                    city.addPopulation(-removedBuilding.getPRODUCTION());
                }else{
                    //TODO: Is this correct? The production of a custom house depends on the number of Ports around it,
                    // so I'm not sure this should be a constant value.
                    city.subtractProduction(removedBuilding.getPRODUCTION());
                }
                // TODO: Should be check if the building enum is changed
                if (removedBuilding.getTYPE().getKey() >= Types.BUILDING.TEMPLE.getKey()) {
                    gs.getTribe(city.getTribeId()).subtractScore(removedBuilding.getPoints());
                }

                boolean isTemple = removedBuilding.getTYPE().getKey() >= Types.BUILDING.TEMPLE.getKey() && removedBuilding.getTYPE().getKey() <= Types.BUILDING.MOUNTAIN_TEMPLE.getKey();
                if(isTemple){
                    city.subtractLongTermPoints(removedBuilding.getPoints());
                }

                if(removedBuilding.getTYPE() == Types.BUILDING.PORT)
                {
                    //If a port is removed, then the tile stops belonging to the trade network
                    b.setTradeNetwork(position.x, position.y, false);
                }



                return true;
            }
        }
        return false;
    }
}
